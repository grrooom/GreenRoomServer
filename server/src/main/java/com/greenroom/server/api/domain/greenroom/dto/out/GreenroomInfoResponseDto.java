package com.greenroom.server.api.domain.greenroom.dto.out;

import com.greenroom.server.api.domain.greenroom.entity.Activity;
import com.greenroom.server.api.domain.greenroom.entity.GreenRoom;
import com.greenroom.server.api.domain.greenroom.entity.Item;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@Slf4j
public class GreenroomInfoResponseDto {

    private GreenroomBasicInfoDto basicInfo;

    private GreenroomTodoInfoDto todo;

    private Map<String, ItemSimpleDto> customItems;

    public record GreenroomBasicInfoDto(Long greenroomId,String plantNickname,String plantName, String imageUrl,String memo){
        public static GreenroomBasicInfoDto from(GreenRoom greenRoom,String cdnPath){
            return new GreenroomBasicInfoDto(greenRoom.getGreenroomId(), greenRoom.getName(), greenRoom.getPlant()==null?null:greenRoom.getPlant().getCommonName() , greenRoom.getPictureUrl()==null?null:cdnPath+"/"+greenRoom.getPictureUrl() ,greenRoom.getMemo());
        }
    }
    public record GreenroomTodoInfoDto(List<TodoSimpleDto> todoList, Integer numberOfTodo){ }

    public record TodoSimpleDto(Long activityId, String activityName,String description){
        public static TodoSimpleDto from(Activity activity){
            return new TodoSimpleDto(activity.getActivityId(),activity.getActivityName(),activity.getDescription());
        }

    }

    public record ItemSimpleDto(Long itemId, String itemName){
        public static ItemSimpleDto from(Item item){
            return new ItemSimpleDto(item.getItemId(),item.getItemName());
        }
    }


}
