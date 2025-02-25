package com.greenroom.server.api.domain.greenroom.dto.out;

import com.greenroom.server.api.domain.greenroom.document.PlantDocument;
import com.greenroom.server.api.domain.greenroom.entity.Plant;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PlantResponseDto {
    private Long plantId;
    private String plantName;
    private String imageUrl;

    public static PlantResponseDto from(PlantDocument plantDocument, String urlPrefix){
        return new PlantResponseDto(plantDocument.getPlantId(),plantDocument.getCommonName(),urlPrefix+"/"+plantDocument.getPlantPictureUrlS3());
    }

    public static PlantResponseDto from(Plant plant, String urlPrefix){
        return new PlantResponseDto(plant.getPlantId(),plant.getCommonName(),urlPrefix+"/"+plant.getPlantPictureUrlS3());
    }
}
