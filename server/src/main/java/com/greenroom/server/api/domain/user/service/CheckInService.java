package com.greenroom.server.api.domain.user.service;

import com.greenroom.server.api.domain.greenroom.dto.out.PointAndLevelUpResponseDto;
import com.greenroom.server.api.domain.user.entity.CheckIn;
import com.greenroom.server.api.domain.user.entity.User;
import com.greenroom.server.api.domain.user.repository.CheckInRepository;
import com.greenroom.server.api.global.exception.CustomException;
import com.greenroom.server.api.global.response.enums.ResponseCodeEnum;
import com.greenroom.server.api.security.service.CustomUserDetailService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class CheckInService {

    //repository
    private final CheckInRepository checkInRepository;

    //service
    private final GradeService gradeService;
    private final CustomUserDetailService customUserDetailService;

    @Transactional
    public CheckIn createCheckIn(User user){
        CheckIn checkIn = CheckIn.createCheckIn(user);
        checkInRepository.save(checkIn);
        return checkIn;
    }


    @Transactional
    public PointAndLevelUpResponseDto doCheckIn(String email){

        User user = customUserDetailService.findUserByEmail(email);

        // 첫 식물 등록 안한 경우 출석 체크 불가
        if(!user.getIsFirstGreenroomRegistered()){throw new CustomException(ResponseCodeEnum.CHECKED_IN_NOT_ALLOWED);}

        CheckIn checkIn = checkInRepository.findByUser(user).orElseGet(()-> createCheckIn(user));


        if(checkIn.getCheckInDate()==null || !checkIn.getCheckInDate().equals(LocalDate.now())) {
            checkIn.updateCheckinDate();

            user.addTotalSeed(1); //point 증가

            return PointAndLevelUpResponseDto.of(user, 1, gradeService.updateUserGrade(user));
        }
        else{throw new CustomException(ResponseCodeEnum.CHECKED_IN_NOT_ALLOWED);}

    }


}
