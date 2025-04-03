package com.greenroom.server.api.domain.greenroom.dto.out;

import com.greenroom.server.api.domain.greenroom.entity.*;
import com.greenroom.server.api.global.config.PropertiesHolder;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

public record GreenroomCalendarResponseDto(DateInfo dateInfo, List<CalendarInfo> mainInfo) {
    public static GreenroomCalendarResponseDto from(LocalDate date, List<CalendarInfo> mainInfo){
        return new GreenroomCalendarResponseDto(DateInfo.from(date),mainInfo);
    }

    public record CalendarInfo(GreenroomInfo greenroomInfo, List<TodoInfo> activity , List<DiaryInfo> diary){
        public static CalendarInfo of(GreenroomInfo greenroomInfo,List<TodoInfo> activity, List<DiaryInfo> diary){
            return new CalendarInfo(greenroomInfo,activity,diary);
        }
    }

    public record GreenroomInfo(Long greenroomId, String greenroomName){
        public static GreenroomInfo from(GreenRoom greenRoom){
            return new GreenroomInfo(greenRoom.getGreenroomId(),greenRoom.getName());
        }
    }
    public record DateInfo(Integer year, Integer month, Integer date,String day){
        public static DateInfo from(LocalDate date){
            return new DateInfo(date.getYear(),date.getMonthValue(),date.getDayOfMonth(),date.getDayOfWeek()
                    .getDisplayName(TextStyle.SHORT, Locale.KOREAN));
        }
    }
    public record TodoInfo(Long activityId, String activityName, Integer remainingDays, Boolean isCompleted){
        public static TodoInfo from(Todo todo){
            Integer remainingDays = todo.getNextTodoDate().getDayOfYear() - LocalDate.now().getDayOfYear();
            return new TodoInfo(todo.getActivity().getActivityId(),todo.getActivity().getActivityName(),remainingDays,false);
        }
        public static TodoInfo from(TodoLog todoLog){
            Activity activity = todoLog.getActivity();
            return new TodoInfo(activity.getActivityId(),activity.getActivityName(),null,true);
        }
    }
    public record DiaryInfo(Long diaryId, String title, String body, String imageUrl){
        public static DiaryInfo from(Diary diary){
            String imageUrl = diary.getDiaryPictureUrl()==null?null:PropertiesHolder.CDN_PATH+"/"+diary.getDiaryPictureUrl();
            return new DiaryInfo(diary.getDiaryId(),diary.getTitle(),diary.getContent(),imageUrl);
        }
    }
}
