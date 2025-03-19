package com.greenroom.server.api.domain.greenroom.controller;

import com.greenroom.server.api.domain.greenroom.service.PlantService;
import com.greenroom.server.api.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("api/plants/")
public class PlantController {

    private final PlantService plantService;

    //인기 식물 조회
    @GetMapping("popular")
    public ResponseEntity<ApiResponse> getPlantByKeyword(@RequestParam(value = "size",required = false,defaultValue = "-1") Integer size) {
        return ResponseEntity.ok(ApiResponse.success(plantService.getPopularPlantList(size)));
    }

    //키워드로 식물 검색
    @GetMapping("search")
    public ResponseEntity<ApiResponse> getPlantByKeyword(
            @RequestParam(value = "keyword",required = false,defaultValue = " ") String keyWord,
            @RequestParam(value = "size",required = false,defaultValue = "-1") Integer size) {
        return ResponseEntity.ok(ApiResponse.success(plantService.getPlantListWithKeyword(keyWord,size)));
    }

    //식물 물주기 정보 받아오기
    @GetMapping("{plant_id}/watering-info")
    public ResponseEntity<ApiResponse> getWateringInfo(@PathVariable(value = "plant_id")Long plantId){
        return ResponseEntity.ok(ApiResponse.success(plantService.getWateringInfo(plantId)));
    }

    @GetMapping("{plant_id}")
    public ResponseEntity<ApiResponse> getPlantDetailInfo(@PathVariable(value = "plant_id")Long plantId){
        return ResponseEntity.ok(ApiResponse.success(plantService.getPlantDetailInfo(plantId)));
    }
}
