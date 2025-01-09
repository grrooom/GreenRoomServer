package com.greenroom.server.api.security.service;

import com.greenroom.server.api.domain.alram.entity.Alarm;
import com.greenroom.server.api.domain.alram.repository.AlarmRepository;
import com.greenroom.server.api.domain.greenroom.repository.GradeRepository;
import com.greenroom.server.api.domain.user.entity.User;
import com.greenroom.server.api.domain.user.enums.Role;
import com.greenroom.server.api.domain.user.enums.UserStatus;
import com.greenroom.server.api.domain.user.repository.UserRepository;
import com.greenroom.server.api.enums.ResponseCodeEnum;
import com.greenroom.server.api.exception.CustomException;
import com.greenroom.server.api.security.dto.SignupRequestDto;
import com.greenroom.server.api.security.dto.TokenDto;
import com.greenroom.server.api.security.entity.EmailVerificationLogs;
import com.greenroom.server.api.security.entity.RefreshToken;
import com.greenroom.server.api.security.enums.VerificationStatus;
import com.greenroom.server.api.security.repository.EmailVerificationLogsRepository;
import com.greenroom.server.api.security.repository.RefreshTokenRepository;
import com.greenroom.server.api.security.util.TokenProvider;
import com.greenroom.server.api.utils.MailSender;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.lang.Strings;
import io.jsonwebtoken.security.SignatureException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;

/**
 * 순수 user 관련 생명 주기만 관리 하는 서비스
 * (인증, 인가, 생성, 삭제)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CustomUserDetailService implements UserDetailsService {
    private final AlarmRepository alarmRepository;

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final TokenProvider tokenProvider;
    private final MailSender mailSender;
    private final GradeRepository gradeRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationLogsRepository emailVerificationLogsRepository;

    private static final Map<Role,List<GrantedAuthority>> authorityMap = new HashMap<>();

    @Override
    public UserDetails loadUserByUsername(final String email) throws UsernameNotFoundException {
        return getUserDetails(findUserByEmail(email));
    }

    private UserDetails getUserDetails(User user){
        return new org.springframework.security.core.userdetails.User(user.getEmail(), user.getPassword(), authorityMap.get(user.getRole()));
    }

    /**
     * token 상태 업데이트
     */
    public User findUserByEmail(final String userEmail){
        return userRepository
                .findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("해당 user가 존재 하지 않습니다."));
    }

    @Transactional
    public TokenDto issueAllTokens(Authentication authentication){
        return tokenProvider.createAllToken(authentication);
    }


    //accessToken & refresh token update
    @Transactional
    public TokenDto updateToken(String refreshToken){

        String email;
        try {
            email = tokenProvider.findUser(refreshToken);
        }
        catch (ExpiredJwtException e){
            throw new CustomException(ResponseCodeEnum.REFRESH_TOKEN_EXPIRED,"refresh token이 만료되었음.");
        }
        catch (UnsupportedJwtException|MalformedJwtException|SignatureException|IllegalArgumentException e){
            throw new CustomException(ResponseCodeEnum.REFRESH_TOKEN_INVALID,e.getMessage());
        }

        //저장된 refresh token 없으면 다시 로그인 요구
        RefreshToken userSavedRefreshToken = refreshTokenRepository.findRefreshTokenByEmail(email).orElseThrow(()-> new CustomException(ResponseCodeEnum.REFRESH_TOKEN_NOT_EXISTS,"사용자에게 저장된 refresh token이 존재하지 않아 access token을 갱신할 수 없습니다. 다시 로그인 해 주세요."));


        //이상 없음.
        if(refreshToken.equals(userSavedRefreshToken.getRefreshToken())){

            User user = findUserByEmail(email);

            List<GrantedAuthority> authorities = authorityMap.get(user.getRole());

            // Authentication 객체 생성
            org.springframework.security.core.userdetails.User principal = new org.springframework.security.core.userdetails.User(email,"password",authorities);
            Authentication authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities);

            //새로운 토큰 발급
            TokenDto tokenDto = issueAllTokens(authentication);
            String newRefreshToken = tokenDto.getRefreshToken();

            //새로 발급된 refresh token으로 update
            userSavedRefreshToken.updateRefreshToken(newRefreshToken);
            refreshTokenRepository.save(userSavedRefreshToken);

            return tokenDto;
        }

        //이상 있음
        // 1. 탈취당한 예전 access token을 가지고 refresh token이 갱신이 됐을 때, 원래 사용자의 refresh token과 맞지 않음
        // 2. 이미 accessToken과 refreshToken이 갱신 되었는데, 그 전에 탈취한 token을 가지고 갱신을 시도하는 경우, 갱신된 refreshToken과 일치하지 않음.
        else{
            //저장되어 있는 refresh token 삭제
            refreshTokenRepository.deleteRefreshTokenByEmail(email);
            throw new CustomException(ResponseCodeEnum.REFRESH_TOKEN_NOT_MATCHED,"저장된 refresh token과 요청된 refresh token이 일치하지 않습니다. access token을 갱신할 수 없습니다.");
        }
    }

    //일반 email,password 회원가입
    @Transactional
    public void save(SignupRequestDto signupRequestDto){

        String email = signupRequestDto.getEmail();
        String password = passwordEncoder.encode(signupRequestDto.getPassword());

        Optional <EmailVerificationLogs> eLog =  emailVerificationLogsRepository.findByEmail(email);

        //이메일 인증을 시도한적 없거나 시도했으나 아직 인증상태가 아닌 경우 에러 반환
        if(eLog.isEmpty() || !eLog.get().getVerificationStatus().equals(VerificationStatus.VERIFIED)){
            throw new CustomException(ResponseCodeEnum.EMAIL_NOT_VERIFIED);
        }

        Optional<User> findUser = userRepository.findByEmail(email);

        // 이미 가입된 회원 정보가 있음.
        if(findUser.isPresent()){

            User user = findUser.get();

            //이메일로 가입된 적 있는 경우 에러 반환
            if(!user.getPassword().isEmpty()){
                throw new CustomException(ResponseCodeEnum.USER_ALREADY_EXIST,"해당 email로 가입된 user가 이미 존재");
            }

            //소셜 로그인으로만 가입이 되어 있는 경우
            else {
                throw new CustomException(ResponseCodeEnum.OAUTH_USER_ALREADY_EXIST_CONNECT_AVAILABLE,"연동 가능한 oauth user가 존재.");
            }
        }
        // 이미 가입된 회원 정보가 없다면 새로 등록
        else{
            User user = User.createUser(new SignupRequestDto(email,password),gradeRepository.findByLevel(0).orElse(null));
            userRepository.save(user);
            alarmRepository.save(Alarm.builder().user(user).build());
        }
    }

    @Transactional
    public TokenDto login(String email,String password){

        User user = findUserByEmail(email);
        //로그인 정보가 일치
        if(passwordEncoder.matches(password,user.getPassword())){
            List<GrantedAuthority> authorities = authorityMap.get(user.getRole());

            org.springframework.security.core.userdetails.User principal = new org.springframework.security.core.userdetails.User(email,"password",authorities);
            Authentication authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities);

            TokenDto tokenDto =  issueAllTokens(authentication);
            String refreshToken = tokenDto.getRefreshToken();

            Optional<RefreshToken> refreshTokenOptional =  refreshTokenRepository.findRefreshTokenByEmail(email);

            //저장되어 있는 refresh token이 있으면 update
            if(refreshTokenOptional.isPresent()){
                refreshTokenOptional.get().updateRefreshToken(refreshToken);
            }
            //없으면 새로 생성.
            else{
                refreshTokenRepository.save(RefreshToken.builder().email(email).refreshToken(refreshToken).build());
            }

            //새로운 토큰 발급
            return tokenDto;
        }
        //로그인 정보가 불일치
        else{
            throw new CustomException(ResponseCodeEnum.PASSWORD_NOT_MATCHED);
        }
    }

    @Transactional
    public void emailAuthentication(String redirectUrl, String email){

        Optional<EmailVerificationLogs> optionalEmailLog =  emailVerificationLogsRepository.findByEmail(email);

        if(userRepository.findByEmail(email).isPresent() ){
            throw new CustomException(ResponseCodeEnum.VERIFIED_USER_ALREADY_EXISTS,"이미 가입된 email을 가지고 인증을 시도함.");
        }
        String token = null;

        if(optionalEmailLog.isPresent()){

            EmailVerificationLogs emailLog = optionalEmailLog.get();

            if(emailLog.getVerificationStatus().equals(VerificationStatus.VERIFIED)){
                throw new CustomException(ResponseCodeEnum.ALREADY_VERIFIED_EMAIL,"이미 인증된 email을 가지고 인증을 시도함.");
            }

            //5회 이상 시도했을 경우
            if(emailLog.getNumberOfTrial()>=5){
                // 15분 이상 지났을 경우 초기화
                if(emailLog.getExpiresAt().plusMinutes(15).isBefore(LocalDateTime.now())){
                    token = tokenProvider.createVerificationToken(email);
                    emailLog.updateLog(token);
                }
                //15분 안 지났을 경우 차단
                else{throw new CustomException(ResponseCodeEnum.EXCEED_NUMBER_OF_TRIAL_VERIFICATION,"인증 횟수를 초과하여 더 이상 인증이 불가능함. 15분 후에 다시 시도 가능");}
            }
            //5회 미만일 경우 요청 승인 & 횟수 증가
            else{
                token = tokenProvider.createVerificationToken(email);
                emailLog.updateLog(token);
            }
        }
        //처음 시도한 경우 새로 생성
        else{

            if(!isValidEmail((email))){throw  new CustomException(ResponseCodeEnum.INVALID_EMAIL_FORMAT);}

            token = tokenProvider.createVerificationToken(email);
            emailVerificationLogsRepository.save(EmailVerificationLogs.createLog(email,1,token));
        }


        String deepLink = redirectUrl+"?token="+token;
        mailSender.sendEmail(email,deepLink);

    }


    public Boolean isValidEmail(String email){
        final String EMAIL_REGEX =
                "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        if (email == null || email.isEmpty()) {
            return false; // null 또는 빈 값 처리
        }
        return Pattern.matches(EMAIL_REGEX, email);
    }

    @Transactional
    public void verifyEmailToken(String token){

        Optional<EmailVerificationLogs> optionalEmailLog = emailVerificationLogsRepository.findByVerificationToken(token);

        if(optionalEmailLog.isPresent()){
            EmailVerificationLogs emailLog = optionalEmailLog.get();

            //토큰 만료 안되었으면
            if(LocalDateTime.now().isBefore(emailLog.getExpiresAt())){
                emailLog.updateVerificationStatus(VerificationStatus.VERIFIED);
                emailVerificationLogsRepository.save(emailLog);
            }
            //토큰 만료 되었으면
            else {throw new CustomException(ResponseCodeEnum.EMAIL_VERIFICATION_CODE_EXPIRED,"email 인증 token이 만료됨.");}
        }
        //인증 token 일치하지 않음.
        else{throw new CustomException(ResponseCodeEnum.EMAIL_VERIFICATION_CODE_NOT_MATCHED,"email 인증을 시도한적 없거나 인증 token이 일치하지 않음.");}
    }

    @EventListener(ApplicationReadyEvent.class)
    public void setAuthoritiesMap(){
        List<GrantedAuthority> guestAuthorities = new ArrayList<GrantedAuthority>();
        guestAuthorities.add(new SimpleGrantedAuthority("GUEST"));

        List<GrantedAuthority> generalAuthorities = new ArrayList<GrantedAuthority>();
        generalAuthorities.add(new SimpleGrantedAuthority("GUEST"));
        generalAuthorities.add(new SimpleGrantedAuthority("GENERAL"));

        List<GrantedAuthority> adminAuthorities = new ArrayList<GrantedAuthority>();
        adminAuthorities.add(new SimpleGrantedAuthority("GUEST"));
        adminAuthorities.add(new SimpleGrantedAuthority("GENERAL"));
        adminAuthorities.add(new SimpleGrantedAuthority("ADMIN"));

        authorityMap.put(Role.GUEST,guestAuthorities);
        authorityMap.put(Role.GENERAL,generalAuthorities);
        authorityMap.put(Role.ADMIN,adminAuthorities);
    }
}
