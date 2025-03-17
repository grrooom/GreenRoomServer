package com.greenroom.server.api.domain.greenroom.dto.out;

import com.greenroom.server.api.domain.greenroom.entity.GreenRoom;
import com.greenroom.server.api.domain.greenroom.entity.Plant;
import com.greenroom.server.api.domain.greenroom.entity.Todo;
import lombok.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;


public record GreenroomDetailResponseDto(
        GreenroomBasicInfoDto basicInfo,
        Map<String, ItemSimpleDto> decoration,
        List<GreenroomManagementInfoDto> managementInfo,
        PlantInfoDto plantInfo,
        PlantManagementInfoDto plantManagementInfo
) {

    public static GreenroomDetailResponseDto of(
            GreenroomBasicInfoDto basicInfo,
            Map<String, ItemSimpleDto> decoration,
            List<GreenroomManagementInfoDto> managementInfo,
            PlantInfoDto plantInfo,
            PlantManagementInfoDto plantManagementInfo
    ) {
        return new GreenroomDetailResponseDto(basicInfo, decoration, managementInfo, plantInfo, plantManagementInfo);
    }

    public record GreenroomBasicInfoDto (Long greenroomId, String nickName, String plantName, Integer duration, String memo, String imageUrl){
        public static GreenroomBasicInfoDto from(GreenRoom greenRoom,String cdnPath){
            String completeImageUrl = greenRoom.getPictureUrl()==null? null: cdnPath+"/"+greenRoom.getPictureUrl();
            return new GreenroomBasicInfoDto(greenRoom.getGreenroomId(),greenRoom.getName(), greenRoom.getPlant()==null?null:greenRoom.getPlant().getCommonName(),LocalDate.now().getDayOfYear()- greenRoom.getCreateDate().getDayOfYear()+1,greenRoom.getMemo(), completeImageUrl);
        }
    }

    public record GreenroomManagementInfoDto (Long activityId, String activityName,Integer term,Integer remainingDays){
        public static GreenroomManagementInfoDto from(Todo todo){
            return new GreenroomManagementInfoDto(todo.getActivity().getActivityId(),todo.getActivity().getActivityName(),todo.getTerm(),todo.getNextTodoDate().getDayOfYear()-LocalDate.now().getDayOfYear());}
    }

    public record PlantInfoDto (Long plantId,String name,String scientificName,String description){
        public static PlantInfoDto from(Plant plant){
            return new PlantInfoDto(plant.getPlantId(),plant.getCommonName(),plant.getScientificName(),plant.getOtherInformation());
        }
    }

    public record PlantManagementInfoDto (String managementLevel,String temperature,String sunlight,String watering,String humidity,String fertilizer){
        public static PlantManagementInfoDto from(Plant plant){
            return new PlantManagementInfoDto(plant.getManageLevel(),plant.getGrowthTemperature(), plant.getLightDemand(), plant.getWaterCycle(), plant.getHumidity(),plant.getFertilizer());
        }
    }

}
