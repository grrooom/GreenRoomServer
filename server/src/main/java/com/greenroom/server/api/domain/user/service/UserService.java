package com.greenroom.server.api.domain.user.service;

import com.greenroom.server.api.domain.alram.repository.AlarmRepository;
import com.greenroom.server.api.domain.greenroom.repository.*;
import com.greenroom.server.api.domain.user.dto.UserExitReasonResponseDto;
import com.greenroom.server.api.domain.user.dto.UserExitRequestDto;
import com.greenroom.server.api.domain.user.entity.User;
import com.greenroom.server.api.domain.user.enums.UserStatus;
import com.greenroom.server.api.domain.user.repository.UserRepository;
import com.greenroom.server.api.security.repository.EmailVerificationLogsRepository;
import com.greenroom.server.api.security.repository.RefreshTokenRepository;
import com.greenroom.server.api.security.service.CustomUserDetailService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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
    private final AlarmRepository alarmRepository;


    private final CustomUserDetailService customUserDetailService;
    private final UserExitReasonService userExitReasonService;

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
        alarmRepository.deleteByUser(user);

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



}
