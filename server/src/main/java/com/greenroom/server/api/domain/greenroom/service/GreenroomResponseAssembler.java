package com.greenroom.server.api.domain.greenroom.service;

import com.greenroom.server.api.domain.greenroom.dto.out.GreenroomDetailResponseDto;
import com.greenroom.server.api.domain.greenroom.dto.out.GreenroomInfoResponseDto;
import com.greenroom.server.api.domain.greenroom.dto.out.ItemSimpleDto;
import com.greenroom.server.api.domain.greenroom.entity.GreenRoom;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class GreenroomResponseAssembler {

    @Value("${cloud.cdn.path.root}")
    private  String cdnPath;

    private final AdornmentService adornmentService;
    private final TodoService todoService;

    public GreenroomResponseAssembler(AdornmentService adornmentService, TodoService todoService) {
        this.adornmentService = adornmentService;
        this.todoService = todoService;
    }

    public GreenroomDetailResponseDto toDetailResponse(GreenRoom greenRoom) {

        GreenroomDetailResponseDto.GreenroomBasicInfoDto greenroomBasicInfoDto = GreenroomDetailResponseDto.GreenroomBasicInfoDto.from(greenRoom,cdnPath);

        Map<String, ItemSimpleDto> decoration = adornmentService.getGreenroomSimpleAdornmentInfo(greenRoom);

        List<GreenroomDetailResponseDto.GreenroomManagementInfoDto> managementInfo = todoService.getGreenroomManagementInfo(greenRoom);

        GreenroomDetailResponseDto.PlantInfoDto plantInfo = GreenroomDetailResponseDto.PlantInfoDto.from(greenRoom.getPlant());

        GreenroomDetailResponseDto.PlantManagementInfoDto plantManagementInfo = GreenroomDetailResponseDto.PlantManagementInfoDto.from(greenRoom.getPlant());

        return GreenroomDetailResponseDto.of(greenroomBasicInfoDto,decoration,managementInfo,plantInfo,plantManagementInfo);
    }

    public GreenroomInfoResponseDto toGreenroomInfo(GreenRoom greenRoom){

        //user의 greenroom 기본 정보 조회
        GreenroomInfoResponseDto.GreenroomBasicInfoDto  greenroomBasicInfo=  GreenroomInfoResponseDto.GreenroomBasicInfoDto.from(greenRoom,cdnPath);

        //user의 greenroom todo 조회
        GreenroomInfoResponseDto.GreenroomTodoInfoDto greenroomTodoInfo =  todoService.getGreenroomTodoInfo(greenRoom);

        //greenroom item 조회
        Map<String, ItemSimpleDto> greenroomItemInfo = adornmentService.getGreenroomFullAdornmentInfo(greenRoom);

        return new GreenroomInfoResponseDto(greenroomBasicInfo,greenroomTodoInfo,greenroomItemInfo);

    }


}
