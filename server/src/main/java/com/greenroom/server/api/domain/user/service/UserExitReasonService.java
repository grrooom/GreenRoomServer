package com.greenroom.server.api.domain.user.service;

import com.greenroom.server.api.domain.user.dto.UserExitReasonResponseDto;
import com.greenroom.server.api.domain.user.dto.UserExitRequestDto;
import com.greenroom.server.api.domain.user.entity.UserExitReason;
import com.greenroom.server.api.domain.user.enums.UserExitReasonType;
import com.greenroom.server.api.domain.user.repository.UserExitReasonRepository;
import jakarta.transaction.Transactional;
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

    @Transactional
    public void saveUserExitReason(UserExitRequestDto userExitRequestDto){

         // null 또는 비어있는 list 허용 o, 처리 x
        if(userExitRequestDto.getReasonIdList()!=null && !userExitRequestDto.getReasonIdList().isEmpty()){
            List<Long> reasonIdList = userExitRequestDto.getReasonIdList();
            // 탈퇴 사유 횟수 늘리기
            userExitReasonRepository.findAllById(reasonIdList).forEach(UserExitReason::updateCount);
        }

        // 기타 사유 있으면 저장하기
        // null 또는 빈값(""," " 등의 공백)일 경우 저장 x 허용 o
       if(userExitRequestDto.getCustomReason()!=null && !userExitRequestDto.getCustomReason().isBlank()){
           userExitReasonRepository.save(UserExitReason.createCustomReason(userExitRequestDto.getCustomReason()));
       }
    }

    public List<UserExitReasonResponseDto> getDefinedUserExitReasons(){
        return userExitReasonRepository.findAllByReasonType(UserExitReasonType.DEFINED).stream().map(UserExitReasonResponseDto::from).collect(Collectors.toList());
    }

}
