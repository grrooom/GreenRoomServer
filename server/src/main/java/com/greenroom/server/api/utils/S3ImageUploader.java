package com.greenroom.server.api.utils;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.greenroom.server.api.global.response.enums.ResponseCodeEnum;
import com.greenroom.server.api.global.exception.CustomException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.amazonaws.util.IOUtils.toByteArray;

@Component
@Slf4j
@RequiredArgsConstructor
public class S3ImageUploader {

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.image.path.user}")
    private String userImageDir;

    @Value("${cloud.image.path.greenroom}")
    private String greenroomImageDir;

    @Value("${cloud.image.path.greenroom}")
    private String diaryImageDir;

    @Value("${cloud.image.path.plant}")
    private String plantImageDir;

    private final AmazonS3 amazonS3;

    @Getter
    @Value("${cloud.cdn.path.root}")
    private String cdnPath;

    public String uploadUserProfileImage(MultipartFile multipartFile){
        return uploadImage(multipartFile,userImageDir);
    }

    public String uploadGreenroomImage(MultipartFile multipartFile){
        return uploadImage(multipartFile,greenroomImageDir);
    }

    public String uploadDiaryImage(MultipartFile multipartFile){
        return uploadImage(multipartFile,diaryImageDir);
    }

    public String uploadImage(MultipartFile multipartFile, String dir) {

        String contentType = multipartFile.getContentType();

        //이미지 파일 형식 검증 & 사이즈 검증(10mb 제한)
        if ( !contentType.startsWith("image/") || (float) multipartFile.getSize() /(1024.0*1024.0) > (float) 10) {
            throw new CustomException(ResponseCodeEnum.INVALID_IMAGE_FORMAT);
        }

        UUID uuId = UUID.randomUUID();
        String serverFileName = dir+"/"+ uuId+ "-" + multipartFile.getOriginalFilename();

        //image metadata 설정
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(multipartFile.getSize());
        metadata.setContentType(multipartFile.getContentType());

        int maxRetries = 3; // 최대 재시도 횟수
        int attempt = 0;

        while (true) {
            try (InputStream inputStream = multipartFile.getInputStream()) {
                amazonS3.putObject(bucket, serverFileName, inputStream, metadata);
                break;
            }
            catch (IOException | SdkClientException e){
                attempt++;
                log.warn("[warn] Failed to upload image attempt {}/{}. Error: {}", attempt, maxRetries, e.getMessage());

                if (attempt >= maxRetries) {
                    // 재시도 끝까지 실패하면 예외 던지기
                    throw new CustomException(ResponseCodeEnum.FAIL_TO_UPLOAD_IMAGE);
                }
                try {
                    Thread.sleep(1000L * attempt); // 1초, 2초, 3초... 점진적 딜레이
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt(); // 인터럽트 발생 시 즉시 종료
                    throw new CustomException(ResponseCodeEnum.FAIL_TO_UPLOAD_IMAGE);
                }
            }
        }
        return serverFileName;
    }

    public void deleteImage(String imageFileUrl){

        try {
            amazonS3.deleteObject(bucket,imageFileUrl);
        }
        catch (SdkClientException e){
            //삭제 연산 실패 시 log 남김.
            // 고아 객체 - 낙관적 처리
            //추후 삭제 연산 실패 시 db 저장 -> 삭제 실패한 파일 삭제 재시도 (스케줄러) 도입 가능
            log.error("[error] Fail to delete image files after 3 times retry : {}",imageFileUrl);
        }

    }

    public void deleteImageInBatch(List<String> imageFileUrlList){

        int maxBatchSize = 1000; //한번에 삭제 가능한 파일 개수
        int listLength  = imageFileUrlList.size();
        int startIndex = 0;
        int endIndex = Math.min(listLength,maxBatchSize);

        while(startIndex < listLength){
            List<DeleteObjectsRequest.KeyVersion> keyVersionList = new ArrayList<>();
            imageFileUrlList.subList(startIndex, endIndex).forEach(i-> keyVersionList.add(new DeleteObjectsRequest.KeyVersion(i)));

            deleteWithRetry(keyVersionList);

            startIndex = endIndex;
            endIndex = Math.min(startIndex + maxBatchSize, listLength);
        }

    }

    public void deleteWithRetry(List<DeleteObjectsRequest.KeyVersion> keyList){

        final int MAX_TRY = 3;
        int trial = 1;
        int waitTime = 1000;

        //error가 발생했을 경우 최대 3번까지 재시도 & slow down error(503) 발생했을 경우 지수 백오프
        while(trial <= MAX_TRY){
            try {
                amazonS3.deleteObjects(new DeleteObjectsRequest(bucket).withKeys(keyList));
                log.info("[success] Successfully deleted images: {}", keyList);
                return ;
            }
            catch (AmazonServiceException e){
                //503 error(slow down)  발생
                if(e.getStatusCode()==503){
                    log.warn("[warning] Slow Down error on attempt {}: Retrying after {}ms", trial, waitTime);
                    try {
                        Thread.sleep(waitTime);
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                    }
                    waitTime =  waitTime * 2 ; // 지수 백오프 (1초 → 2초 → 4초)
                }
                //4xx 에러 발생 -> 재시도 x
                else if(e.getStatusCode()>=400 && e.getStatusCode()<500){
                    log.error("[exception] Client error (4xx) while deleting images: {} - {}", keyList, e.getMessage());
                    return ;
                }
                //그 외 에러 발생
                else {log.warn("[warning] AWS service error on attempt {}: {} - Retrying...", trial, e.getMessage());}
            }
            // 네트워크 문제, SDK 오류 시 재시도
            catch (SdkClientException e){
                log.warn("[warning] Network or SDK error on attempt {}: {} - Retrying...", trial, e.getMessage());
            }
            trial ++;
        }
        //삭제 연산 최종 실패 시 log로 남기고 넘어가기
        //추후 삭제 연산 실패 시 db 저장 -> 삭제 실패한 파일 삭제 재시도 (스케줄러) 도입 가능
        log.error("[error] Fail to delete image files after 3 times retry : {}",keyList);
    }

    public String uploadPlantImages(InputStream inputStream, String plantName) throws IOException {

        byte[] bytes = toByteArray(inputStream);
        InputStream newInputStream = new ByteArrayInputStream(bytes);
        long contentLength = bytes.length;

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType("image/jpg");
        metadata.setContentLength(contentLength);
        String fileName = plantImageDir+"/"+plantName+".jpg";
        amazonS3.putObject(bucket,fileName, newInputStream,metadata);

        return fileName;
    }


}
