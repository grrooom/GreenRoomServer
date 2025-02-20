package com.greenroom.server.api.domain.greenroom.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.greenroom.server.api.domain.greenroom.service.PlantService;
import com.greenroom.server.api.utils.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
public class PlantController {

    private final PlantService plantService;

    //인기 식물 조회
    @GetMapping("api/plants/popular")
    public ResponseEntity<ApiResponse> getPlantByKeyword(@RequestParam(value = "size",required = false,defaultValue = "-1") String size) {
        return ResponseEntity.ok(ApiResponse.success(plantService.getPopularPlantList(size)));
    }

    //키워드로 식물 검색
    @GetMapping("api/plants/search")
    public ResponseEntity<ApiResponse> getPlantByKeyword(
            @RequestParam(value = "keyword",required = false,defaultValue = " ") String keyWord,
            @RequestParam(value = "size",required = false,defaultValue = "-1") String size) {
        return ResponseEntity.ok(ApiResponse.success(plantService.getPlantListWithKeyword(keyWord,size)));
    }
}
