package com.greenroom.server.api.domain.greenroom.dto.out;


import com.greenroom.server.api.domain.greenroom.entity.Plant;

public record PlantDetailInfoResponseDto(PlantInfoDto plantInfo, PlantManagementInfoDto plantManagementInfo) {
    public static PlantDetailInfoResponseDto from(Plant plant){
        return new PlantDetailInfoResponseDto(PlantInfoDto.from(plant),PlantManagementInfoDto.from(plant));
    }
}
