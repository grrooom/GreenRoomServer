package com.greenroom.server.api.domain.greenroom.dto.out;

public record PlantWateringInfoResponseDto (
    Long plantId,
    String plantName,
    String wateringInfo){

    public static PlantWateringInfoResponseDto of(Long plantId,String plantName, String wateringInfo){
        return new PlantWateringInfoResponseDto(plantId,plantName,wateringInfo);
    }
}
