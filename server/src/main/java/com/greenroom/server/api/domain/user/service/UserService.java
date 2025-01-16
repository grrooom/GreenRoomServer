package com.greenroom.server.api.domain.user.service;

import com.greenroom.server.api.domain.user.entity.User;
import com.greenroom.server.api.domain.user.enums.UserStatus;
import com.greenroom.server.api.domain.user.repository.UserRepository;
import com.greenroom.server.api.security.repository.EmailVerificationLogsRepository;
import com.greenroom.server.api.security.repository.RefreshTokenRepository;
import com.greenroom.server.api.security.service.CustomUserDetailService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;


@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final EmailVerificationLogsRepository emailVerificationLogsRepository;
    private final RefreshTokenRepository refreshTokenRepository;


    //회원 탈퇴
    @Transactional
    public void deactivateUser(String email){

        User user = findUserByEmail(email);
        //1. email 인증 로그 삭제
        if(emailVerificationLogsRepository.existsByEmail(email)){emailVerificationLogsRepository.deleteByEmail(email);}
        //2. refresh token 삭제
        if(refreshTokenRepository.existsByEmail(email)){refreshTokenRepository.deleteRefreshTokenByEmail(email);}
        //2. user 삭제-대기 상태로 전환
        user.withdrawalUser();
    }

    //회원 삭제
    @Transactional
    public void deleteUser(User user){
        //연관관계 객체는 추후 추가
        userRepository.delete(user);
    }


    public User findUserByEmail(final String userEmail){

        return userRepository.findUserByEmailAndUserStatus(userEmail, UserStatus.IN_ACTION).orElseThrow(()->new UsernameNotFoundException("해당 user가 존재 하지 않습니다."));
    }



}
