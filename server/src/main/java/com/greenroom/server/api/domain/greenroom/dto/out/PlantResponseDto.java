package com.greenroom.server.api.domain.greenroom.dto.out;

import com.greenroom.server.api.domain.greenroom.document.PlantDocument;
import com.greenroom.server.api.domain.greenroom.entity.Plant;
public record PlantResponseDto (
    Long plantId,
    String plantName,
    String imageUrl){

    public static PlantResponseDto of(PlantDocument plantDocument, String urlPrefix){
        return new PlantResponseDto(plantDocument.getPlantId(),plantDocument.getCommonName(),urlPrefix+"/"+plantDocument.getPlantPictureUrlS3());
    }

    public static PlantResponseDto of(Plant plant, String urlPrefix){
        return new PlantResponseDto(plant.getPlantId(),plant.getCommonName(),urlPrefix+"/"+plant.getPlantPictureUrlS3());
    }
}
