package com.greenroom.server.api.domain.greenroom.dto.out;

import com.greenroom.server.api.domain.greenroom.entity.Plant;
import com.greenroom.server.api.global.properties.PropertiesHolder;

public record PlantInfoDto (Long plantId, String name, String scientificName, String description, String imageUrl){

    public static PlantInfoDto from(Plant plant){
        return new PlantInfoDto(plant.getPlantId(),plant.getCommonName(),plant.getScientificName(),plant.getOtherInformation(), PropertiesHolder.CDN_PATH +"/"+ plant.getPlantPictureUrlS3());
    }
}