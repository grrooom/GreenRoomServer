package com.greenroom.server.api.domain.greenroom.dto.out;

import com.greenroom.server.api.domain.greenroom.entity.Diary;
import com.greenroom.server.api.global.properties.PropertiesHolder;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
public class DiaryResponseDto {
    private final Long greenroomId;
    private final String greenroomName;
    private final Long diaryId;
    private final String title;
    private final String content;
    private final String imageUrl;
    private final LocalDateTime dateTime;

    public static DiaryResponseDto from(Diary diary){
        String imageUrl =null;

        if(diary.getDiaryPictureUrl()!=null){imageUrl = PropertiesHolder.CDN_PATH+"/"+diary.getDiaryPictureUrl();}

        LocalDateTime createTime = diary.getCreateDate()==null?LocalDateTime.now():diary.getCreateDate();
        LocalTime time = LocalTime.of(createTime.getHour(),createTime.getMinute());
        LocalDateTime diaryDateTime = LocalDateTime.of(diary.getDate(),time);

        return new DiaryResponseDto(diary.getGreenRoom().getGreenroomId(),diary.getGreenRoom().getName(), diary.getDiaryId(), diary.getTitle(), diary.getContent(), imageUrl, diaryDateTime);
    }
}
