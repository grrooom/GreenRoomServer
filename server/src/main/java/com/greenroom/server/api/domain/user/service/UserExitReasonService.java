package com.greenroom.server.api.domain.user.service;

import com.greenroom.server.api.domain.user.dto.UserExitReasonResponseDto;
import com.greenroom.server.api.domain.user.dto.UserExitRequestDto;
import com.greenroom.server.api.domain.user.entity.UserExitReason;
import com.greenroom.server.api.domain.user.enums.UserExitReasonType;
import com.greenroom.server.api.domain.user.repository.UserExitReasonRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserExitReasonService {

    private final UserExitReasonRepository userExitReasonRepository;
    public void saveUserExitReason(UserExitRequestDto userExitRequestDto){

        List<Long> reasonIdList = userExitRequestDto.getReasonIdList();

        // 탈퇴 사유 횟수 늘리기
       userExitReasonRepository.findAllById(reasonIdList).forEach(UserExitReason::updateCount);

       // 기타 사유 있으면 저장하기
       if(!userExitRequestDto.getCustomReason().isEmpty()){
           userExitReasonRepository.save(UserExitReason.createCustomReason(userExitRequestDto.getCustomReason()));
       }
    }

    public List<UserExitReasonResponseDto> getDefinedUserExitReasons(){
        return userExitReasonRepository.findAllByReasonType(UserExitReasonType.DEFINED).stream().map(UserExitReasonResponseDto::from).collect(Collectors.toList());
    }

}
