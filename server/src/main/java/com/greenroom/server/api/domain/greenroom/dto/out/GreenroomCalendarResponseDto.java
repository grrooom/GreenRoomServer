package com.greenroom.server.api.domain.greenroom.dto.out;

import com.greenroom.server.api.domain.greenroom.entity.*;
import com.greenroom.server.api.global.config.PropertiesHolder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
    public record DiaryInfo(Long diaryId, Long greenroomId, String greenroomName, String title, String body, String imageUrl, LocalDateTime dateTime){
        public static DiaryInfo from(Diary diary){
            String imageUrl = diary.getDiaryPictureUrl()==null?null:PropertiesHolder.CDN_PATH+"/"+diary.getDiaryPictureUrl();
            LocalTime time = LocalTime.of(diary.getCreateDate().getHour(),diary.getCreateDate().getMinute());
            LocalDateTime diaryDateTime = LocalDateTime.of(diary.getDate(),time);
            return new DiaryInfo(diary.getDiaryId(),diary.getGreenRoom().getGreenroomId(),diary.getGreenRoom().getName(),diary.getTitle(),diary.getContent(),imageUrl,diaryDateTime);
        }
    }
}
