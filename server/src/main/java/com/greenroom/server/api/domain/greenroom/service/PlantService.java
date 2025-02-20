package com.greenroom.server.api.domain.greenroom.service;

import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.greenroom.server.api.domain.greenroom.document.PlantDocument;
import com.greenroom.server.api.domain.greenroom.dto.PlantResponseDto;
import com.greenroom.server.api.domain.greenroom.entity.Plant;
import com.greenroom.server.api.domain.greenroom.repository.PlantDocumentRepository;
import com.greenroom.server.api.domain.greenroom.repository.PlantRepository;
import com.greenroom.server.api.domain.greenroom.utils.GardeningDataUtil;
import com.greenroom.server.api.enums.ResponseCodeEnum;
import com.greenroom.server.api.exception.CustomException;
import com.greenroom.server.api.utils.S3ImageUploader;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@RequiredArgsConstructor
@Service
@Slf4j
@Transactional
public class PlantService {
    //util
    private final GardeningDataUtil gardeningDataUtil;
    private final S3ImageUploader s3ImageUploader;

    //repository
    private final PlantRepository plantRepository;
    private final PlantDocumentRepository plantDocumentRepository;

    @Value("${cloud.cdn.path.root}")
    private String cdnPath;

    public void insertPlantDataIntoDB() throws JsonProcessingException {

        Map<String, ArrayList<String>> plantList = gardeningDataUtil.plantList();
        JSONArray objects = gardeningDataUtil.plantInfo(plantList);
        ObjectMapper objectMapper = new ObjectMapper();

        log.info("db insert 중");
        for(int i=0;i<objects.length();i++) {
            Plant plant = objectMapper.readValue(objects.getJSONObject(i).toString(), Plant.class);
            plantRepository.save(plant);
        }
    }

    public void deleteData(){
        plantRepository.deleteAllInBatch();
    }

    public void uploadPlantImageToS3() throws IOException {
        List<Plant> plantList =  plantRepository.findAll();

        for(Plant plant : plantList){
            String imageUrl = plant.getPlantPictureUrl();

            if(imageUrl==null){continue;}
            RestTemplate restTemplate = new RestTemplate();
            Resource resource = restTemplate.getForObject(imageUrl, Resource.class);
            InputStream inputStream = resource.getInputStream();
            String fileName = imageUrl.replace("https://nongsaro.go.kr/cms_contents/301/","").split("\\.")[0];
            String s3FileName = s3ImageUploader.uploadPlantImages(inputStream,fileName);
            plant.updateS3PlantPictureUrl(s3FileName);
        }
    }

    public List<PlantResponseDto> getPlantListWithKeyword(String keyWord, String size){

        String stn = keyWord.substring(0,1);


        try{Integer.parseInt(size);}
        catch (NumberFormatException e){throw new CustomException(ResponseCodeEnum.INVALID_REQUEST_PARAM,"올바르지 않은 request param이 전달됨");}

        int limit  = Integer.parseInt(size);

        try{
            return plantDocumentRepository.findPlantDocumentByCommonName(keyWord)
                    .stream()
                    .sorted(Comparator.comparingInt(r -> r.getCommonName().indexOf(stn))) // DTO 매핑 전에 정렬
                    .limit(limit != -1 ? limit : Long.MAX_VALUE) // 필요할 때만 limit 제한 적용
                    .map(plantDocument -> PlantResponseDto.from(plantDocument, cdnPath)) // 정렬 후 필요한 만큼만 매핑
                    .toList();
        }
        catch (ElasticsearchException e){
            throw new CustomException(ResponseCodeEnum.FAIL_TO_SEARCH_WITH_ELASTICSEARCH,e.getMessage());
        }
    }

    public List<PlantResponseDto> getPopularPlantList(String size){

        try{Integer.parseInt(size);}
        catch (NumberFormatException e){throw new CustomException(ResponseCodeEnum.INVALID_REQUEST_PARAM,"올바르지 않은 request param이 전달됨");}

        int limit  = Integer.parseInt(size);

        return plantRepository.findAll()
                .stream()
                .sorted((p1,p2)->p2.getPlantCount()-p1.getPlantCount())
                .limit(limit != -1 ? limit : Long.MAX_VALUE) // 필요할 때만 limit 제한 적용
                .map(plant -> PlantResponseDto.from(plant, cdnPath)) // 정렬 후 필요한 만큼만 매핑
                .toList();

    }



    public void updatePlantDocumentWithPlant(){
        List<PlantDocument> plantDocumentList =  plantRepository.findAll().stream().map(p-> new PlantDocument(p.getPlantId(),p.getCommonName(),p.getScientificName(),p.getPlantPictureUrlS3())).toList();
        plantDocumentRepository.saveAll(plantDocumentList);
    }

}