package com.greenroom.server.api.domain.user.service;

import com.amazonaws.util.StringUtils;
import com.greenroom.server.api.domain.greenroom.entity.GreenRoom;
import com.greenroom.server.api.domain.greenroom.service.*;
import com.greenroom.server.api.domain.notification.repository.NotificationRepository;
import com.greenroom.server.api.domain.greenroom.repository.*;
import com.greenroom.server.api.domain.user.dto.*;
import com.greenroom.server.api.domain.user.entity.User;
import com.greenroom.server.api.domain.user.enums.UserStatus;
import com.greenroom.server.api.domain.user.repository.GradeRepository;
import com.greenroom.server.api.domain.user.repository.UserRepository;
import com.greenroom.server.api.security.repository.EmailVerificationLogsRepository;
import com.greenroom.server.api.security.repository.RefreshTokenRepository;
import com.greenroom.server.api.security.service.CustomUserDetailService;
import com.greenroom.server.api.utils.S3ImageUploader;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;


@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final EmailVerificationLogsRepository emailVerificationLogsRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final GreenRoomRepository greenRoomRepository;
    private final NotificationRepository notificationRepository;
    private final GradeRepository gradeRepository;


    private final CustomUserDetailService customUserDetailService;
    private final UserExitReasonService userExitReasonService;
    private final S3ImageUploader s3ImageUploader;
    private final GreenroomService greenroomService;

    @Value("${cloud.cdn.path.root}")
    private String cdnRoot;

    // 회원 로그아웃 : 저장되어 있는 refresh token 삭제
    @Transactional
    public void logout(String email){
        User user = customUserDetailService.findUserByEmail(email); //user 없으면 error 반환
        //저장되어 있는 refresh token 있으면 삭제
        refreshTokenRepository.deleteRefreshTokenByUser(user);
    }

    //탈퇴 사유 불러오기
    public List<UserExitReasonResponseDto> getDefinedUserExitReasons(){
        return userExitReasonService.getDefinedUserExitReasons();
    }

    //회원 탈퇴 : 탈퇴 사유 정리 & 삭제 대기 상태 전환
    @Transactional
    public void deactivateUser(String email, UserExitRequestDto userExitRequestDto){

        User user = customUserDetailService.findUserByEmail(email); //user 없으면 error 반환
        //1. email 인증 로그 삭제
        emailVerificationLogsRepository.deleteByEmail(email);
        //2. refresh token 삭제
        refreshTokenRepository.deleteRefreshTokenByUser(user);
        //2. user 삭제-대기 상태로 전환
        user.deactivateUser();
        //3. 회원 탈퇴 이유 처리
        userExitReasonService.saveUserExitReason(userExitRequestDto);
    }

    //회원 삭제 hard delete
    @Transactional
    public List<String> deleteAllWithUser(User user){

        List<String> imageDeleteList = new ArrayList<>();

        //user 관련 greenroom 조회 & greenroom 이미지 삭제 대상에 포함
        imageDeleteList.addAll(deleteAllGreenroomWithUser(user));

        imageDeleteList.addAll(deleteUserData(user));

        return imageDeleteList;
    }

    public List<String> deleteUserData(User user){
        //alarm 삭제
        notificationRepository.deleteByUser(user);
        //user 객체 삭제
        userRepository.delete(user);

        if(StringUtils.hasValue(user.getProfileUrl()))  return List.of(user.getProfileUrl());
        else return List.of();
    }

    @Transactional
    public List<String> deleteAllGreenroomWithUser(User user){

        List<Long> greenroomIdList = greenRoomRepository.findAllByUser(user).stream().map(GreenRoom::getGreenroomId).toList();

        return greenroomService.deleteGreenroom(greenroomIdList);
    }


    @Transactional
    @Scheduled(cron = "0 0 3 * * ?")
    public void deleteUserHard(){

        log.info("[alert] User Deletion Scheduler has started running");

        LocalDateTime threshold = LocalDateTime.now().minusDays(90); // 90일 경과한 데이터 삭제
        List<User> users =  userRepository.findAllByUserStatusAndDeleteDateBefore(UserStatus.DELETE_PENDING,threshold);
        List<String> imageDeleteList = new ArrayList<>();
        users.forEach(user->imageDeleteList.addAll(deleteAllWithUser(user)));

        log.info("[alert] {} users deleted successfully", users.size());

        //이미지 모두 삭제 (batch 삭제)
        s3ImageUploader.deleteImageInBatch(imageDeleteList);
    }

    public UserInfoResponseDto getUserInformation(String email){

        //User Not Found

        User user = customUserDetailService.findUserByEmail(email); //없으면 not found error반환

        LocalDateTime userJoinedDate = user.getCreateDate();
        Long userDurationWithGreenroom = ChronoUnit.DAYS.between(userJoinedDate.toLocalDate(), LocalDate.now());

        int nextGradeRequiredSeed = gradeRepository.findById(user.getGrade().getGradeId()+1).get().getRequiredSeed();
        int seedsToNextGrade = nextGradeRequiredSeed -  user.getGrade().getRequiredSeed() ;

        String imagePrefix = cdnRoot+"/";

       return  UserInfoResponseDto.from(user,seedsToNextGrade,userDurationWithGreenroom, imagePrefix);

    }

    @Transactional
    public UserNameUpdateDto updateUserName(String email, String name){

        //UserNotFound

        User user = customUserDetailService.findUserByEmail(email); //없으면 not found error 발생

        user.updateUserName(name);

        return new UserNameUpdateDto(user.getName());
    }

    @Transactional
    public void deleteUserProfileImage(String email){

        //UserNotFound
        User user = customUserDetailService.findUserByEmail(email); //없으면 not found error 발생

        String imageFileUrl = user.getProfileUrl();

        user.deleteProfileImage();
        //s3 버킷에서 삭제
        if(StringUtils.hasValue(imageFileUrl)) s3ImageUploader.deleteImage(imageFileUrl);

    }

    @Transactional
    public UserProfileImageResponseDto uploadUserProfileImage(String email, MultipartFile multipartFile){

        //UserNotFound , FAIL_TO_UPLOAD_IMAGE, INVALID_IMAGE_FORMAT
        User user = customUserDetailService.findUserByEmail(email);

        String imageUrl = s3ImageUploader.uploadUserProfileImage(multipartFile);

        user.updateProfileUrl(imageUrl);

        return new UserProfileImageResponseDto(cdnRoot+"/"+imageUrl);

    }

}
