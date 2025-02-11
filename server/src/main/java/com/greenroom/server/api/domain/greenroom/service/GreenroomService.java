package com.greenroom.server.api.domain.greenroom.service;

import com.greenroom.server.api.domain.greenroom.dto.GreenroomInfoResponseDto;
import com.greenroom.server.api.domain.greenroom.entity.GreenRoom;
import com.greenroom.server.api.domain.greenroom.enums.GreenRoomStatus;
import com.greenroom.server.api.domain.greenroom.repository.GreenRoomRepository;
import com.greenroom.server.api.domain.user.entity.User;
import com.greenroom.server.api.security.service.CustomUserDetailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GreenroomService {

    private final CustomUserDetailService customUserDetailService;
    private final GreenRoomRepository greenRoomRepository;

    private final TodoService todoService;
    private final AdornmentService adornmentService;
    public GreenroomInfoResponseDto getGreenroomInfo(String email){

        User user = customUserDetailService.findUserByEmail(email); //없으면 NOT_FOUND 예외 발생

        //user의 greenroom 조회
        List<GreenRoom> greenRoomList =  greenRoomRepository.findGreenRoomByUserAndGreenroomStatus(user, GreenRoomStatus.ENABLED);

        //등록된 식물이 없으면 null 반환
        if(greenRoomList.isEmpty()){
            return null;
        }

        GreenRoom greenroom = greenRoomList.get(0);
        //user의 greenroom 기본 정보 조회
        GreenroomInfoResponseDto.GreenroomBasicInfoDto  greenroomBasicInfo=  GreenroomInfoResponseDto.GreenroomBasicInfoDto.from(greenroom);

        //user의 greenroom todo 조회
        GreenroomInfoResponseDto.GreenroomTodoInfoDto greenroomTodoInfo =  todoService.getGreenroomTodoInfo(greenroom);

        //greenroom item 조회
        Map<String, GreenroomInfoResponseDto.ItemSimpleDto> greenroomItemInfo = adornmentService.getGreenroomAdornmentInfo(greenroom);

        return new GreenroomInfoResponseDto(greenroomBasicInfo,greenroomTodoInfo,greenroomItemInfo);


    }



}

