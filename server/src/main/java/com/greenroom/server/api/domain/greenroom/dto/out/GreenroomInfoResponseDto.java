package com.greenroom.server.api.domain.greenroom.dto.out;

import com.greenroom.server.api.domain.greenroom.entity.Activity;
import com.greenroom.server.api.domain.greenroom.entity.GreenRoom;
import com.greenroom.server.api.global.properties.PropertiesHolder;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;


@Slf4j
public record GreenroomInfoResponseDto (
    GreenroomBasicInfoDto basicInfo,
    GreenroomTodoInfoDto todo,
   Map<String, ItemSimpleDto> customItems) {
    public static GreenroomInfoResponseDto of(GreenroomBasicInfoDto basicInfo, GreenroomTodoInfoDto todo, Map<String, ItemSimpleDto> customItems){
        return new GreenroomInfoResponseDto(basicInfo,todo,customItems);
    }
    public record GreenroomBasicInfoDto(Long greenroomId,String plantNickname,String plantName, String imageUrl,String memo){
        public static GreenroomBasicInfoDto from(GreenRoom greenRoom){
            return new GreenroomBasicInfoDto(greenRoom.getGreenroomId(), greenRoom.getName(), greenRoom.getPlant()==null?null:greenRoom.getPlant().getCommonName() , greenRoom.getPictureUrl()==null?null: PropertiesHolder.CDN_PATH+"/"+greenRoom.getPictureUrl() ,greenRoom.getMemo());
        }
    }
    public record GreenroomTodoInfoDto(List<TodoSimpleDto> todoList, Integer numberOfTodo){
        public static GreenroomTodoInfoDto of(List<TodoSimpleDto> todoList, Integer numberOfTodo){
            return new GreenroomTodoInfoDto(todoList, numberOfTodo);}
    }

    public record TodoSimpleDto(Long activityId, String activityName,String description){
        public static TodoSimpleDto from(Activity activity){
            return new TodoSimpleDto(activity.getActivityId(),activity.getActivityName(),activity.getDescription());}
    }
}
