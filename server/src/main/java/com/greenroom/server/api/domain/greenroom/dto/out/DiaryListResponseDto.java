package com.greenroom.server.api.domain.greenroom.dto.out;


import com.greenroom.server.api.domain.greenroom.entity.Diary;
import com.greenroom.server.api.global.config.PropertiesHolder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

public record DiaryListResponseDto(List<DateAndDiaryList> diaryList) {
    public static DiaryListResponseDto of(List<DateAndDiaryList> diaryList){
        return new DiaryListResponseDto(diaryList);
    }

    public record DateAndDiaryList(DateInfo dateInfo, List<DiaryInfo> diaryInfo){
        public static DateAndDiaryList of(DateInfo dateInfo, List<DiaryInfo> diaryInfo){
            return new DateAndDiaryList(dateInfo,diaryInfo);
        }
    }

    public record DateInfo(Integer year, Integer month, Integer date, String day){
        public static DateInfo from(LocalDate date){
            return new DateInfo(date.getYear(),date.getMonthValue(),date.getDayOfMonth(),date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.KOREAN));
        }
    }

    public record DiaryInfo(Long diaryId, Long greenroomId, String greenroomName, String title, String content, String imageUrl, LocalDateTime dateTime){
        public static DiaryInfo from(Diary diary){

            String imageUrl = null;
            if(diary.getDiaryPictureUrl()!=null){imageUrl= PropertiesHolder.CDN_PATH+"/"+ diary.getDiaryPictureUrl();}


            LocalTime time = LocalTime.of(diary.getCreateDate().getHour(),diary.getCreateDate().getMinute());
            LocalDateTime diaryDateTime = LocalDateTime.of(diary.getDate(),time);

            return new DiaryInfo(diary.getDiaryId(),diary.getGreenRoom().getGreenroomId(),diary.getGreenRoom().getName(), diary.getTitle(), diary.getContent(),imageUrl,diaryDateTime);
        }
    }

}
