package com.greenroom.server.api.domain.greenroom.service;

import com.amazonaws.util.StringUtils;
import com.greenroom.server.api.domain.greenroom.dto.in.*;
import com.greenroom.server.api.domain.greenroom.dto.out.*;
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
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GreenroomService {

    //repository
    private final GreenRoomRepository greenRoomRepository;

    //service
    private final CustomUserDetailService customUserDetailService;
    private final TodoService todoService;
    private final TodoLogService todoLogService;
    private final AdornmentService adornmentService;
    private final S3ImageUploader s3ImageUploader;
    private final PlantService plantService;
    private final GradeService gradeService;
    private final DiaryService diaryService;

    //util
    private final GreenroomResponseAssembler greenroomResponseAssembler;

    public GreenRoom findEnabledGreenroomById(Long greenRoomId){
        return greenRoomRepository.findByGreenroomIdAndGreenroomStatus(greenRoomId,GreenRoomStatus.ENABLED).orElseThrow(()->new CustomException(ResponseCodeEnum.GREENROOM_NOT_FOUND));
    }


    public GreenroomInfoResponseDto getGreenroomInfo(String email){

        User user = customUserDetailService.findUserByEmail(email); //없으면 NOT_FOUND 예외 발생

        List<GreenRoom> greenRoomList =  greenRoomRepository.findGreenRoomByUserAndGreenroomStatus(user, GreenRoomStatus.ENABLED);

        if(greenRoomList.isEmpty()){return null;}

        return greenroomResponseAssembler.toGreenroomInfo(greenRoomList.get(0));

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
        LocalDate wateringBaseTime;
        try{
            wateringBaseTime =LocalDate.parse(greenroomRegistrationRequestDto.getWateringBaseDate());
        }
        catch (DateTimeParseException e){throw new CustomException(ResponseCodeEnum.INVALID_REQUEST_ARGUMENT);}

        // 그린룸 등록
        User user = customUserDetailService.findUserByEmail(email);  // 없으면 not found
        Plant plant = greenroomRegistrationRequestDto.getPlantId()==null?null:plantService.findPlantById(greenroomRegistrationRequestDto.getPlantId());  //없으면 not found
        String imageFileName = imageFile==null ||imageFile.isEmpty()?null:s3ImageUploader.uploadGreenroomImage(imageFile);  //FAIL_TO_UPLOAD_IMAGE , //INVALID_IMAGE_FORMAT
        GreenRoom greenRoom =  GreenRoom.of(greenroomRegistrationRequestDto.getNickname(),imageFileName,user,plant);
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

    public Map<String,ItemSimpleDto> updateGreenroomAdornment(Long greenroomId, GreenroomDecorationRequestDto greenroomDecorationRequestDto){

        GreenRoom greenRoom = findEnabledGreenroomById(greenroomId); // 없으면 not found

        return adornmentService.updateAdornment(greenRoom, greenroomDecorationRequestDto);

    }

    public GreenroomDetailResponseDto getGreenroomDetails(Long greenroomId){

        GreenRoom greenRoom = findEnabledGreenroomById(greenroomId); // 없으면 not found exception 발생

        return greenroomResponseAssembler.toDetailResponse(greenRoom);
    }

    @Transactional
    public List<String> deleteGreenroom(List<Long> greenroomIdList){

        List<String> imageDeleteList = new ArrayList<>();

        greenRoomRepository.findAllById(greenroomIdList).forEach(greenRoom -> {
            if(greenRoom.getPictureUrl()!=null){imageDeleteList.add(greenRoom.getPictureUrl());}}
        );

        //greenroom 연관 adornment 객체 삭제
        adornmentService.deleteAllByGreenRoom(greenroomIdList);

        //greenroom 연관 diary 객체 삭제  + diary 객체 image 파일 삭제 대상에 포함.
        imageDeleteList.addAll(diaryService.deleteAllByGreenRoom(greenroomIdList));

        // greenroom 연관된 todo_log, todo 삭제
        todoLogService.deleteAllByGreenroom(greenroomIdList);
        todoService.deleteAllByGreenRoom(greenroomIdList);

        // greenroom 삭제
        greenRoomRepository.deleteAllByIdInBatch(greenroomIdList);

        return imageDeleteList;
    }

    @Transactional
    public void deleteAllGreenroomAndDeleteAllImages(List<Long> greenroomIdList){

        s3ImageUploader.deleteImageInBatch(deleteGreenroom(greenroomIdList));
    }

    @Transactional
    public void postMemo(Long greenroomId,MemoRequestDto memoRequestDto){

        if(isOver30Characters(memoRequestDto.memo())){throw new CustomException(ResponseCodeEnum.TOO_LONG_STRING);}

        GreenRoom greenRoom = findEnabledGreenroomById(greenroomId);
        greenRoom.updateMemo(memoRequestDto.memo());
    }

    public boolean isOver30Characters(String str) {
        return str.length() > 30;
    }

    @Transactional
    public GreenroomDetailResponseDto registerGreenroomPlant(Long greenroomId, GreenroomPlantRequestDto greenroomPlantRequestDto){

        GreenRoom greenRoom =  findEnabledGreenroomById(greenroomId); //없으면 not found

        Plant plant ;
        if(greenroomPlantRequestDto.plantId()==null){plant=null;}
        else{plant= plantService.findPlantById(greenroomPlantRequestDto.plantId());}

        greenRoom.updatePlant(plant);

        return getGreenroomDetails(greenroomId);
    }

    @Transactional
    public GreenroomDetailResponseDto updateGreenroomInfo(Long greenroomId, GreenroomInfoUpdateRequestDto greenroomInfoUpdateRequestDto, MultipartFile imageFile){

        GreenRoom  greenRoom = findEnabledGreenroomById(greenroomId); //없으면 not found
        GreenRoomStatus greenRoomStatus = greenroomInfoUpdateRequestDto.isAlive()?GreenRoomStatus.ENABLED:GreenRoomStatus.DISABLED;

        greenRoom.updateName(greenroomInfoUpdateRequestDto.nickname()); //이름 변경
        greenRoom.updateStatus(greenRoomStatus); // 상태 변경

        if(greenroomInfoUpdateRequestDto.imageAction()== GreenroomInfoUpdateRequestDto.ImageAction.DELETE){
            deleteGreenroomImage(greenRoom);
        }
        else if(greenroomInfoUpdateRequestDto.imageAction()== GreenroomInfoUpdateRequestDto.ImageAction.UPLOAD){
            updateGreenroomImage(greenRoom,imageFile);
        }
        return getGreenroomDetails(greenroomId);
    }

    private void deleteGreenroomImage(GreenRoom greenRoom){

        String oldImageUrl = greenRoom.getPictureUrl();
        greenRoom.updatePictureUrl(null);

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    if(oldImageUrl!=null){s3ImageUploader.deleteImage(oldImageUrl);}
                }
            });
        }
    }

    private void updateGreenroomImage(GreenRoom greenRoom, MultipartFile imageFile){

        deleteGreenroomImage(greenRoom);

        if(imageFile==null || imageFile.isEmpty()){return;}

        String imageUrl = s3ImageUploader.uploadGreenroomImage(imageFile);
        greenRoom.updatePictureUrl(imageUrl);

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status == TransactionSynchronization.STATUS_ROLLED_BACK && imageUrl!=null) {
                        s3ImageUploader.deleteImage(imageUrl);
                    }
                }
            });
        }
    }

}

