package com.greenroom.server.api.domain.greenroom.dto.out;

import com.greenroom.server.api.domain.greenroom.entity.GreenRoom;
import com.greenroom.server.api.domain.greenroom.entity.Todo;
import com.greenroom.server.api.global.config.PropertiesHolder;

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
        public static GreenroomBasicInfoDto from(GreenRoom greenRoom){
            String completeImageUrl = greenRoom.getPictureUrl()==null? null: PropertiesHolder.CDN_PATH+"/"+greenRoom.getPictureUrl();
            return new GreenroomBasicInfoDto(greenRoom.getGreenroomId(),greenRoom.getName(), greenRoom.getPlant()==null?null:greenRoom.getPlant().getCommonName(),LocalDate.now().getDayOfYear()- greenRoom.getCreateDate().getDayOfYear()+1,greenRoom.getMemo(), completeImageUrl);
        }
    }

    public record GreenroomManagementInfoDto (Long activityId, String activityName,Integer term,Integer remainingDays){
        public static GreenroomManagementInfoDto from(Todo todo){
            return new GreenroomManagementInfoDto(todo.getActivity().getActivityId(),todo.getActivity().getActivityName(),todo.getTerm(),todo.getNextTodoDate().getDayOfYear()-LocalDate.now().getDayOfYear());}
    }

}
