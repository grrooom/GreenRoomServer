package com.greenroom.server.api.domain.greenroom.service;

import com.amazonaws.util.StringUtils;
import com.greenroom.server.api.domain.greenroom.entity.GreenRoom;
import com.greenroom.server.api.domain.greenroom.repository.DiaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
}
