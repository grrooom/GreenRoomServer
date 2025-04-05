package com.greenroom.server.api.domain.greenroom.service;

import com.amazonaws.util.StringUtils;
import com.greenroom.server.api.domain.greenroom.dto.in.DiaryCreationRequestDto;
import com.greenroom.server.api.domain.greenroom.dto.out.DiaryResponseDto;
import com.greenroom.server.api.domain.greenroom.entity.Diary;
import com.greenroom.server.api.domain.greenroom.entity.GreenRoom;
import com.greenroom.server.api.domain.greenroom.repository.DiaryRepository;
import com.greenroom.server.api.domain.user.entity.User;
import com.greenroom.server.api.global.exception.CustomException;
import com.greenroom.server.api.global.response.enums.ResponseCodeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class DiaryService {

    //repository
    private final DiaryRepository diaryRepository;

    public List<String> deleteAllByGreenRoom(List<Long> greenroomIdList){

        List<Long> deletedDiaryIdList = new ArrayList<>();
        List<String> imageDeleteList = new ArrayList<>();
        diaryRepository.findAllByGreenRoomIn(greenroomIdList).forEach(d-> {
            deletedDiaryIdList.add(d.getDiaryId());
            if(StringUtils.hasValue(d.getDiaryPictureUrl())) imageDeleteList.add(d.getDiaryPictureUrl());
        });
        diaryRepository.deleteAllByIdInBatch(deletedDiaryIdList);
        return  imageDeleteList;
    }

    public List<Diary> getAllDiariesByGreenroomAndDate(List<GreenRoom>greenRoomList, LocalDate date){
        return diaryRepository.findByDateAndGreenRoomIn(date,greenRoomList.stream().map(GreenRoom::getGreenroomId).toList());
    }

    public Diary createDiary(GreenRoom greenRoom, DiaryCreationRequestDto request, String imageUrl){
        LocalDate date;
        try{
            date = LocalDate.parse(request.getDate());
        }
        catch (DateTimeParseException e){
            throw new CustomException(ResponseCodeEnum.INVALID_REQUEST_ARGUMENT);
        }

        Diary diary = Diary.createDiary(request.getTitle(),request.getContent(),greenRoom,imageUrl,date);
        return diaryRepository.save(diary);
    }

    public List<Diary> getAllDiariesByUser(User user, YearMonth date){
        return diaryRepository.findByUserAndDate(user, date.getYear(),date.getMonthValue());
    }

    public DiaryResponseDto getSpecificDiary(Long diaryId,String userEmail){
        Diary diary = diaryRepository.findByDiaryId(diaryId).orElseThrow(()-> new CustomException(ResponseCodeEnum.DIARY_NOT_FOUND));

        if(!diary.getGreenRoom().getUser().getEmail().equals(userEmail)){throw new CustomException(ResponseCodeEnum.NOT_AUTHORIZATION);}

        return DiaryResponseDto.from(diary);
    }
}
