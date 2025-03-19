package com.greenroom.server.api.domain.greenroom.dto.out;

import com.greenroom.server.api.domain.greenroom.entity.Plant;


public record PlantManagementInfoDto (String managementLevel,String temperature,String sunlight,String watering,String humidity,String fertilizer){
    public static PlantManagementInfoDto from(Plant plant){
        return new PlantManagementInfoDto(plant.getManageLevel(),plant.getGrowthTemperature(), plant.getLightDemand(), plant.getWaterCycle(), plant.getHumidity(),plant.getFertilizer());
    }
}