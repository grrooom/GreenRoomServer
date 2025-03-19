package com.greenroom.server.api.domain.greenroom.dto.out;

import com.greenroom.server.api.domain.greenroom.entity.Activity;
import com.greenroom.server.api.domain.greenroom.entity.Todo;

import java.time.LocalDate;
import java.util.List;

public record GreenroomTodoCycleResponseDto(
        List<TodoCycleInfo> activeCycle,
        List<TodoSimpleInfo> inactiveCycle
) {

    public record TodoSimpleInfo(Long activityId, String activityName){
        public static TodoSimpleInfo of(Long activityId, String activityName){
            return new TodoSimpleInfo(activityId,activityName);
        }
    }

    public record TodoCycleInfo(Long activityId, String activityName, LocalDate lastDate, Integer term){
        public static TodoCycleInfo from(Todo todo){
            return new TodoCycleInfo(todo.getActivity().getActivityId(),todo.getActivity().getActivityName(),todo.getBaseDate(), todo.getTerm());
        }
    }

    public static GreenroomTodoCycleResponseDto of(List<TodoCycleInfo> activeCycle,List<TodoSimpleInfo> inactiveCycle){
        return new GreenroomTodoCycleResponseDto(activeCycle,inactiveCycle);
    }
}
