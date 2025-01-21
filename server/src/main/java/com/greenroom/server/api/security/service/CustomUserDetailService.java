package com.greenroom.server.api.security.service;

import com.greenroom.server.api.domain.alram.entity.Alarm;
import com.greenroom.server.api.domain.alram.repository.AlarmRepository;
import com.greenroom.server.api.domain.greenroom.repository.GradeRepository;
import com.greenroom.server.api.domain.user.entity.User;
import com.greenroom.server.api.domain.user.enums.Role;
import com.greenroom.server.api.domain.user.enums.UserStatus;
import com.greenroom.server.api.domain.user.repository.UserRepository;
import com.greenroom.server.api.domain.user.service.UserService;
import com.greenroom.server.api.enums.ResponseCodeEnum;
import com.greenroom.server.api.exception.CustomException;
import com.greenroom.server.api.security.dto.SignupRequestDto;
import com.greenroom.server.api.security.dto.TokenDto;
import com.greenroom.server.api.security.entity.EmailVerificationLogs;
import com.greenroom.server.api.security.entity.RefreshToken;
import com.greenroom.server.api.security.enums.VerificationStatus;
import com.greenroom.server.api.security.exception.JWTCustomException;
import com.greenroom.server.api.security.repository.EmailVerificationLogsRepository;
import com.greenroom.server.api.security.repository.RefreshTokenRepository;
import com.greenroom.server.api.security.util.TokenProvider;
import com.greenroom.server.api.utils.MailSender;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.lang.Strings;
import io.jsonwebtoken.security.SecurityException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${greenroom.domain}")
    String domain;

    //spring application이 모두 세팅돼서 시작할 준비를 마쳤을 때, authorityMap의 값이 세팅됨
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

        return userRepository.findUserByEmailAndUserStatus(userEmail,UserStatus.IN_ACTION).orElseThrow(()->new UsernameNotFoundException("해당 user가 존재 하지 않습니다."));
    }


    public Optional<User> findOptionalUserByEmail(String userEmail){
        return userRepository.findUserByEmailAndUserStatus(userEmail,UserStatus.IN_ACTION);

    }

    public Optional<User> findDeletePendingUserByEmail(String email){
        return userRepository.findUserByEmailAndUserStatus(email,UserStatus.DELETE_PENDING);
    }


    public TokenDto issueAllTokens(Authentication authentication){
        return tokenProvider.createAllToken(authentication);
    }

    //accessToken & refresh token update
    @Transactional
    public TokenDto updateToken(String refreshToken) {

        //유효한 refresh token인지 확인 & parsing
        String email = getPrincipalFromRefreshToken(refreshToken);

        //존재하는 회원의 refresh token인지 확인 : ex. 이미 탈퇴한 회원의 유효한 refresh token일 경우 에러 반환
        User user = findUserByEmail(email);

        Optional<RefreshToken> refreshTokenOptional= refreshTokenRepository.findRefreshTokenByUser(user);

        //이상 없음 -> 새로 발급된 refresh token으로 update
        if(refreshTokenOptional.isPresent() && refreshTokenOptional.get().getRefreshToken().equals(refreshToken)){
            TokenDto tokenDto = createToken(user);
            String newRefreshToken = tokenDto.getRefreshToken();
            refreshTokenOptional.get().updateRefreshToken(newRefreshToken);
            return tokenDto;
        }

        if(refreshTokenOptional.isPresent()) {refreshTokenRepository.deleteRefreshTokenByUser(user);}

        throw new CustomException(ResponseCodeEnum.REFRESH_TOKEN_NOT_MATCHED,"저장된 refresh token과 요청된 refresh token의 정보가 일치하지 않습니다.");
    }

    //일반 email,password 회원가입
    @Transactional
    public void save(SignupRequestDto signupRequestDto){

        String email = signupRequestDto.getEmail();
        isValidEmail(email); //email 검증
        String password = passwordEncoder.encode(signupRequestDto.getPassword());

        Optional <EmailVerificationLogs> eLog =  emailVerificationLogsRepository.findByEmail(email);
        if(eLog.isEmpty() || !eLog.get().getVerificationStatus().equals(VerificationStatus.VERIFIED)){
            throw new CustomException(ResponseCodeEnum.EMAIL_NOT_VERIFIED); //이메일 인증을 시도한적 없거나 시도했으나 아직 인증상태가 아닌 경우 에러 반환
        }

        Optional<User> findUser = findOptionalUserByEmail(email); //현재 활동중인 user 검색(삭제 대기중 포함 x)

        //이메일로 가입된 적 있는 경우 에러 반환
        if(findUser.isPresent() && !findUser.get().getPassword().isEmpty()){throw new CustomException(ResponseCodeEnum.USER_ALREADY_EXIST,"해당 email로 가입된 user가 이미 존재");}
        //소셜 로그인으로만 가입이 되어 있는 경우
        else if(findUser.isPresent()) {throw new CustomException(ResponseCodeEnum.OAUTH_USER_ALREADY_EXIST_CONNECT_AVAILABLE,"연동 가능한 oauth user가 존재.");}

        User user = User.createUser(new SignupRequestDto(email,password),gradeRepository.findByLevel(0).orElse(null));
        userRepository.save(user);
        alarmRepository.save(Alarm.builder().user(user).build());
    }

    @Transactional
    public TokenDto login(String email,String password){

        isValidEmail(email);
        User user = findUserByEmail(email);

        //로그인 정보가 불일치
        if(!passwordEncoder.matches(password,user.getPassword())){throw new CustomException(ResponseCodeEnum.PASSWORD_NOT_MATCHED);}

        TokenDto tokenDto = createToken(user);
        String refreshToken = tokenDto.getRefreshToken();

        Optional<RefreshToken> refreshTokenOptional =  refreshTokenRepository.findRefreshTokenByUser(user);

        //저장되어 있는 refresh token이 있으면 update
        if(refreshTokenOptional.isPresent()){refreshTokenOptional.get().updateRefreshToken(refreshToken);}
        //없으면 새로 생성.
        else{refreshTokenRepository.save(RefreshToken.createRefreshToken(user,refreshToken));}

        return tokenDto;
    }


    @Transactional
    public void emailAuthentication(String email){

        //email 유효한지 다시 확인
        isValidEmail(email);

        if(userRepository.existsByEmail(email)){throw new CustomException(ResponseCodeEnum.VERIFIED_USER_ALREADY_EXISTS,"이미 가입된 email을 가지고 인증을 시도함.");}

        String token ;
        Optional<EmailVerificationLogs> optionalEmailLog =  emailVerificationLogsRepository.findByEmail(email);
        if(optionalEmailLog.isPresent()){
            EmailVerificationLogs emailLog = optionalEmailLog.get();

            if(emailLog.getVerificationStatus().equals(VerificationStatus.VERIFIED)){
                throw new CustomException(ResponseCodeEnum.ALREADY_VERIFIED_EMAIL,"이미 인증된 email을 가지고 인증을 시도함.");
            }

            //5회 이상 시도했고, 마지막 인증 시도로부터 15분이 지나지 않았을 경우
            if(emailLog.getNumberOfTrial()>=5 && !emailLog.getUpdateDate().plusMinutes(15).isBefore(LocalDateTime.now())){
                throw new CustomException(ResponseCodeEnum.EXCEED_NUMBER_OF_TRIAL_VERIFICATION,"인증 횟수를 초과하여 더 이상 인증이 불가능함. 잠시 뒤 다시 시도 가능");
            }
            // 인증 시도 횟수가 5회 미만 또는 5회 이상인데 마지막 인증 시도로부터 15분이 지났을 경우
            // 새로 발급해서 token update
            token = tokenProvider.createVerificationToken(email);
            emailLog.updateLog(token);
        }
        //처음 시도한 경우 새로 생성
        else{
            token = tokenProvider.createVerificationToken(email);
            emailVerificationLogsRepository.save(EmailVerificationLogs.createLog(email,token));
        }
        String appLink = domain+"?token="+token;
        mailSender.sendEmail(email,appLink);
    }

    @Transactional
    public void verifyEmailToken(String token){

        //토큰 검증 : 만료 여부
        isValidEmailVerificationToken(token);

        Optional<EmailVerificationLogs> optionalEmailLog = emailVerificationLogsRepository.findByVerificationToken(token);

        //인증 token 일치하지 않거나 존재하지 않음.
        if(optionalEmailLog.isEmpty()){throw new CustomException(ResponseCodeEnum.EMAIL_VERIFICATION_CODE_NOT_MATCHED,"email 인증 token이 일치하지 않음.");}

        // 인증 상태 변경
        optionalEmailLog.get().updateVerificationStatus(VerificationStatus.VERIFIED);
    }


    public void isValidEmail(String email){
        final String EMAIL_REGEX =  "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";

        if(email==null || email.isEmpty() || !Pattern.matches(EMAIL_REGEX, email)){
            throw new CustomException(ResponseCodeEnum.INVALID_REQUEST_ARGUMENT);
        }
    }

    public void isValidEmailVerificationToken(String token){
        try{
            tokenProvider.validateToken(token);
        }
        catch (SecurityException | MalformedJwtException | UnsupportedJwtException | IllegalArgumentException e) {
            throw new CustomException(ResponseCodeEnum.EMAIL_VERIFICATION_CODE_NOT_MATCHED,"인증 token이 일치하지 않음.");
        } catch (ExpiredJwtException e) {
            throw new CustomException(ResponseCodeEnum.EMAIL_VERIFICATION_CODE_EXPIRED,"email 인증 token이 만료됨.");
        }
    }


    public String getPrincipalFromRefreshToken(String refreshToken){
        try{
            return tokenProvider.getPrincipalEmail(refreshToken);
        }
        catch (SecurityException | MalformedJwtException | UnsupportedJwtException | IllegalArgumentException e) {
            throw new CustomException(ResponseCodeEnum.REFRESH_TOKEN_NOT_MATCHED,e.getMessage());
        } catch (ExpiredJwtException e) {
            throw new CustomException(ResponseCodeEnum.REFRESH_TOKEN_EXPIRED,"refresh token이 만료되었음.");
        }
    }

    public TokenDto createToken(User user){
        List<GrantedAuthority> authorities = authorityMap.get(user.getRole());

        // Authentication 객체 생성
        org.springframework.security.core.userdetails.User principal = new org.springframework.security.core.userdetails.User(user.getEmail(),"password",authorities);
        Authentication authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities);

        //새로운 토큰 발급
        return issueAllTokens(authentication);
    }

    // spring은 application이 준비되었을 때 ApplicationReadyEvent 타입의 이벤트를 발행
    // 애플리케이션 컨텍스트 생성 (ApplicationContext), 모든 @Bean 및 @Configuration 초기화 완료,CommandLineRunner 및 ApplicationRunner 실행.
    // 위 과정 완료 후 ApplicationReadyEvent 발행
    // EventListener는 해당 event가 발생했을 때 실행되는 특정 메소드를 실행하도록 event를 listen하는 역할
    //@EventListener(ApplicationReadyEvent.class)
    @PostConstruct
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
