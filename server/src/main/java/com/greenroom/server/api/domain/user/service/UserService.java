package com.greenroom.server.api.domain.user.service;

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
import java.util.List;


@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final EmailVerificationLogsRepository emailVerificationLogsRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TodoLogRepository todoLogRepository;
    private final TodoRepository todoRepository;
    private final DiaryRepository diaryRepository;
    private final AdornmentRepository adornmentRepository;
    private final GreenRoomRepository greenRoomRepository;
    private final NotificationRepository notificationRepository;
    private final GradeRepository gradeRepository;


    private final CustomUserDetailService customUserDetailService;
    private final UserExitReasonService userExitReasonService;
    private final S3ImageUploader s3ImageUploader;

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

    //회원 삭제
    @Transactional
    public void deleteAllWithUser(User user){

        // user hard delete
        List<Long> greenroomIdList = greenRoomRepository.findAllGreenRoomIdByUser(user);

        //모든 greenroom과 연관된 객체 삭제
        adornmentRepository.deleteAllByGreenRoom(greenroomIdList);

        diaryRepository.deleteAllByGreenRoom(greenroomIdList);

        // 모든 greenroom과 연관된 todo_log, todo 삭제
        todoLogRepository.deleteAllByGreenRoom(greenroomIdList);

        todoRepository.deleteAllByGreenRoom(greenroomIdList);

        // greenroom 삭제
        greenRoomRepository.deleteAllByGreenroomId(greenroomIdList);

        //alarm 삭제
        notificationRepository.deleteByUser(user);

        //연관관계 객체는 추후 추가
        userRepository.delete(user);
    }

    @Transactional
    @Scheduled(cron = "0 0 3 * * ?")
    public void deleteUserHard(){

        LocalDateTime threshold = LocalDateTime.now().minusDays(90); // 90일 경과한 데이터 삭제

        List<User> users =  userRepository.findAllByUserStatusAndDeleteDateBefore(UserStatus.DELETE_PENDING,threshold);

        users.forEach(this::deleteAllWithUser);
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

        user.deleteProfileImage();
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
