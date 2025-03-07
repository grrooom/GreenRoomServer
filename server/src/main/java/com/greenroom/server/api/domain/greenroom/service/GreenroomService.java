package com.greenroom.server.api.domain.greenroom.service;

import com.greenroom.server.api.domain.greenroom.dto.in.CompleteTodoRequestDto;
import com.greenroom.server.api.domain.greenroom.dto.in.GreenroomDecorationDTO;
import com.greenroom.server.api.domain.greenroom.dto.out.GreenroomInfoResponseDto;
import com.greenroom.server.api.domain.greenroom.dto.in.GreenroomRegistrationRequestDto;
import com.greenroom.server.api.domain.greenroom.dto.out.PointAndLevelUpResponseDto;
import com.greenroom.server.api.domain.greenroom.entity.GreenRoom;
import com.greenroom.server.api.domain.greenroom.entity.Plant;
import com.greenroom.server.api.domain.greenroom.enums.GreenRoomStatus;
import com.greenroom.server.api.domain.greenroom.repository.GreenRoomRepository;
import com.greenroom.server.api.domain.user.entity.User;
import com.greenroom.server.api.domain.user.service.GradeService;
import com.greenroom.server.api.global.response.enums.ResponseCodeEnum;
import com.greenroom.server.api.global.exception.CustomException;
import com.greenroom.server.api.security.service.CustomUserDetailService;
import com.greenroom.server.api.utils.S3ImageUploader;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GreenroomService {

    @Value("${cloud.cdn.path.root}")
    private  String cdnPath;

    //repository
    private final GreenRoomRepository greenRoomRepository;

    //service
    private final CustomUserDetailService customUserDetailService;
    private final TodoService todoService;
    private final AdornmentService adornmentService;
    private final S3ImageUploader s3ImageUploader;
    private final PlantService plantService;
    private final GradeService gradeService;

    public GreenRoom findEnabledGreenroomById(Long greenRoomId){
        return greenRoomRepository.findByGreenroomIdAndGreenroomStatus(greenRoomId,GreenRoomStatus.ENABLED).orElseThrow(()->new CustomException(ResponseCodeEnum.GREENROOM_NOT_FOUND));
    }


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
        GreenroomInfoResponseDto.GreenroomBasicInfoDto  greenroomBasicInfo=  GreenroomInfoResponseDto.GreenroomBasicInfoDto.from(greenroom,cdnPath);

        //user의 greenroom todo 조회
        GreenroomInfoResponseDto.GreenroomTodoInfoDto greenroomTodoInfo =  todoService.getGreenroomTodoInfo(greenroom);

        //greenroom item 조회
        Map<String, GreenroomInfoResponseDto.ItemSimpleDto> greenroomItemInfo = adornmentService.getGreenroomAdornmentInfo(greenroom);

        return new GreenroomInfoResponseDto(greenroomBasicInfo,greenroomTodoInfo,greenroomItemInfo);


    }

    public Boolean checkDuplication(String email, String nickName){
        User user = customUserDetailService.findUserByEmail(email);

        List<GreenRoom> greenRoomList =  greenRoomRepository.findGreenRoomByUserAndGreenroomStatus(user,GreenRoomStatus.ENABLED);

        for(GreenRoom greenroom : greenRoomList){
            if(greenroom.getName().equals(nickName)) return true;
        }
        return false;
    }

    @Transactional
    public PointAndLevelUpResponseDto createGreenroom(String email, GreenroomRegistrationRequestDto greenroomRegistrationRequestDto, MultipartFile imageFile){

        LocalDate wateringBaseTime =null;

        try{
            wateringBaseTime = LocalDate.parse(greenroomRegistrationRequestDto.getWateringBaseDate());
        } catch (DateTimeParseException e){
            throw  new CustomException(ResponseCodeEnum.INVALID_REQUEST_ARGUMENT);
        }

        // 그린룸 등록
        User user = customUserDetailService.findUserByEmail(email);  // 없으면 not found
        Plant plant = greenroomRegistrationRequestDto.getPlantId()==null?null:plantService.findPlantById(greenroomRegistrationRequestDto.getPlantId());  //없으면 not found
        String imageFileName = imageFile==null ||imageFile.isEmpty()?null:s3ImageUploader.uploadGreenroomImage(imageFile);  //FAIL_TO_UPLOAD_IMAGE , //INVALID_IMAGE_FORMAT
        GreenRoom greenRoom =  GreenRoom.builder()
                .name(greenroomRegistrationRequestDto.getNickname())
                .pictureUrl(imageFileName)
                .user(user)
                .plant(plant)
                .build();
        greenRoomRepository.save(greenRoom);

        // 아이템 등록
        adornmentService.createAdornment(greenRoom, greenroomRegistrationRequestDto.getItemId());//없으면 not found

        // 할 일 등록
        todoService.createWateringTodo(greenroomRegistrationRequestDto.getWateringInterval(), greenRoom,wateringBaseTime);

        //첫 등록일 경우
        if(!user.getIsFirstGreenroomRegistered()){
            user.updateIsFirstGreenroomRegistered(true);
            user.addTotalSeed(2);
            return PointAndLevelUpResponseDto.ofFirstGreenroomRegistration(user,2,gradeService.updateUserGrade(user));
        }

        //첫 식물이 아닐 경우
        else{
            return PointAndLevelUpResponseDto.of(user,0, PointAndLevelUpResponseDto.LevelUpStatus.of(user));
        }
    }


    public PointAndLevelUpResponseDto completeTodo(Long greenroomId, CompleteTodoRequestDto completeTodoRequestDto){

        GreenRoom greenRoom = findEnabledGreenroomById(greenroomId); //없으면 not found

        return todoService.completeTodo(greenRoom,completeTodoRequestDto.getCompletedTodo());

    }

    public Map<String,GreenroomInfoResponseDto.ItemSimpleDto> updateGreenroomAdornment(Long greenroomId, GreenroomDecorationDTO greenroomDecorationDTO){
        GreenRoom greenRoom = findEnabledGreenroomById(greenroomId); // 없으면 not found

        return adornmentService.updateAdornment(greenRoom,greenroomDecorationDTO);

    }

}

