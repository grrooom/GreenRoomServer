package com.greenroom.server.api;

import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.greenroom.server.api.ServerApplication;
import com.greenroom.server.api.domain.user.entity.User;
import com.greenroom.server.api.domain.user.enums.Role;
import com.greenroom.server.api.domain.user.repository.UserRepository;
import com.greenroom.server.api.enums.ResponseCodeEnum;
import com.greenroom.server.api.exception.CustomException;
import com.greenroom.server.api.security.dto.*;
import com.greenroom.server.api.security.entity.EmailVerificationLogs;
import com.greenroom.server.api.security.entity.RefreshToken;
import com.greenroom.server.api.security.enums.VerificationStatus;
import com.greenroom.server.api.security.repository.EmailVerificationLogsRepository;
import com.greenroom.server.api.security.repository.RefreshTokenRepository;
import com.greenroom.server.api.security.service.CustomUserDetailService;
import com.greenroom.server.api.security.util.TokenProvider;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.mockito.Mockito.doThrow;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@AutoConfigureMockMvc
@AutoConfigureRestDocs
@SpringBootTest
public class AuthControllerTest {


    @Autowired
    private MockMvc mockMvc;


    @Autowired
    private CustomUserDetailService userDetailService;

    @MockitoSpyBean
    private CustomUserDetailService customUserDetailService;

    @Autowired
    private EmailVerificationLogsRepository emailVerificationLogsRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TokenProvider tokenProvider;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.secret-key}") String secretKey;

    private final ObjectMapper mapper = new ObjectMapper();

    // enum 설명 적을 때 편리하게 적기 위한 메서드
    private final <E extends Enum<E>> String getEnumValuesAsString(Class<E> enumClass) {
        String enumValues = Arrays.stream(enumClass.getEnumConstants())
                .map(Enum::name)
                .collect(Collectors.joining(", "));
        return " (종류: " + enumValues + ")";
    }

    //  기본 응답 관련해서 공통 descriptor로 처리
    private final List<FieldDescriptor> resultDescriptors = List.of(
            fieldWithPath("status").description("응답 상태")
            ,fieldWithPath("code").description("상태 코드")
            ,fieldWithPath("data").optional().description("data")
    );

    @Test
    @Transactional // 테스트 완료 후 rollback
    @DisplayName("회원가입 api")
    void 회원가입성공() throws Exception {

        //given
        emailVerificationLogsRepository.save(EmailVerificationLogs.builder().email("testEmail@gmail.com").verificationStatus(VerificationStatus.VERIFIED).expires_at(LocalDateTime.now().minusMinutes(15)).build());

        SignupRequestDto signupRequestDto = new SignupRequestDto("testEmail@gmail.com","!123456");

        // when
        ResultActions resultActions = mockMvc.perform( // api 실행
                RestDocumentationRequestBuilders
                        .post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(signupRequestDto))
        );

        // then
        resultActions.andExpect(status().isCreated()); // 상태 코드 created인지 확인

        resultActions.andDo( // 문서 작성
                document(
                        "회원가입-성공", // api의 id
                        resource(
                                ResourceSnippetParameters.builder()
                                        .tag("😎 AUTH-인증/인가") // 문서에서 api들이 태그로 분류됨
                                        .summary("회원가입 요청 api") // api 이름
                                        .description("email과 password로 회원가입을 요청합니다. 인증된 email만 회원가입이 가능합니다.") // api 설명
                                        .responseFields(resultDescriptors) // responseBody 설명
                                        .build()
                        )
                )
        );

    }
    @Test
    @Transactional
    @DisplayName("회원가입 api")
    void 회원가입실패1() throws Exception {

        //given
        SignupRequestDto signupRequestDto = new SignupRequestDto("emailnotvierified@gmail.com", "!123456");

        // when
        ResultActions resultActions = mockMvc.perform( // api 실행
                RestDocumentationRequestBuilders
                        .post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(signupRequestDto))
        );

        // then
        resultActions.andExpect(status().isConflict()).andExpect(jsonPath("code").value("C023"));

        // 문서 작성
        resultActions.andDo(
                document(
                        "회원가입_FAIL_EMAIL_NOT_VERIFIED", // api의 id
                        resource(
                                ResourceSnippetParameters.builder()
                                        .tag("😎 AUTH-인증/인가") // 문서에서 api들이 태그로 분류됨
                                        .summary("회원가입 요청 api") // api 이름
                                        .description("email과 password로 회원가입을 요청합니다. 인증된 email만 회원가입이 가능합니다.") // api 설명
                                        .responseFields(resultDescriptors) // responseBody 설명
                                        .build()
                        )
                )
        );
    }


    @Test
    @Transactional
    @DisplayName("회원가입 api")
    void 회원가입실패2() throws Exception {

        //given
        emailVerificationLogsRepository.save(EmailVerificationLogs.builder().email("testEmail@gmail.com").verificationStatus(VerificationStatus.VERIFIED).expires_at(LocalDateTime.now().minusMinutes(15)).build());
        userDetailService.save(new SignupRequestDto("testEmail@gmail.com", "!123456"));

        SignupRequestDto signupRequestDto = new SignupRequestDto("testEmail@gmail.com", "!123456");

        // when
        ResultActions resultActions = mockMvc.perform( // api 실행
                RestDocumentationRequestBuilders
                        .post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(signupRequestDto))
        );

        // then
        resultActions.andExpect(status().isConflict()).andExpect(jsonPath("code").value("C002")); // 상태 코드 conflict인지 확인

        // 문서 작성
        resultActions.andDo(
                document(
                        "회원가입_FAIL_USER_ALREAD_EXISTS", // api의 id
                        resource(
                                ResourceSnippetParameters.builder()
                                        .tag("😎 AUTH-인증/인가") // 문서에서 api들이 태그로 분류됨
                                        .summary("회원가입 요청 api") // api 이름
                                        .description("이미 존재하는 회원") // api 설명
                                        .responseFields(resultDescriptors) // responseBody 설명
                                        .build()
                        )
                )
        );
    }

    protected List<FieldDescriptor> tokenResultDescriptors = List.of(
            fieldWithPath("status").description("응답 상태")
            ,fieldWithPath("code").description("상태 코드")
            ,fieldWithPath("data").optional().description("data")
            ,fieldWithPath("data.email").description("email")
            ,fieldWithPath("data.accessToken").description("access token")
            ,fieldWithPath("data.refreshToken").description("refresh token")
    );
    @Test
    @Transactional
    @DisplayName("로그인 api")
    void 로그인성공() throws Exception {

        //given
        emailVerificationLogsRepository.save(EmailVerificationLogs.builder().email("testEmail@gmail.com").verificationStatus(VerificationStatus.VERIFIED).expires_at(LocalDateTime.now().minusMinutes(15)).build());
        userDetailService.save(new SignupRequestDto("testEmail@gmail.com", "!123456"));


        LoginRequestDto loginRequestDto = new LoginRequestDto("testEmail@gmail.com", "!123456");

        // when
        ResultActions resultActions = mockMvc.perform( // api 실행
                RestDocumentationRequestBuilders
                        .post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(loginRequestDto))
        );

        // then
        resultActions.andExpect(status().isOk()); // 상태 코드 success인지 확인

        // 문서 작성
        resultActions.andDo(
                document(
                        "로그인-성공", // api의 id
                        resource(
                                ResourceSnippetParameters.builder()
                                        .tag("😎 AUTH-인증/인가") // 문서에서 api들이 태그로 분류됨
                                        .summary("로그인 요청 api") // api 이름
                                        .description("email&password 기반 일반 로그인 api") // api 설명
                                        .responseFields(tokenResultDescriptors) // responseBody 설명
                                        .build()
                        )
                )
        );
    }

    @Test
    @Transactional
    @DisplayName("로그인 api")
    void 로그인실패1() throws Exception {

        //given
        LoginRequestDto loginRequestDto = new LoginRequestDto("testEmail@gmail.com", "!123456");

        // when
        ResultActions resultActions = mockMvc.perform( // api 실행
                RestDocumentationRequestBuilders
                        .post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(loginRequestDto))
        );

        // then
        resultActions.andExpect(status().isNotFound()).andExpect(jsonPath("code").value("C001")); // 상태 코드 not found인지 확인

        // 문서 작성
        resultActions.andDo(
                document(
                        "로그인_FAIL_USER_NOT_FOUND", // api의 id
                        resource(
                                ResourceSnippetParameters.builder()
                                        .tag("😎 AUTH-인증/인가") // 문서에서 api들이 태그로 분류됨
                                        .summary("로그인 요청 api") // api 이름
                                        .description("email&password 기반 일반 로그인 api") // api 설명
                                        .responseFields(resultDescriptors) // responseBody 설명
                                        .build()
                        )
                )
        );
    }

    @Test
    @Transactional
    @DisplayName("로그인 api")
    void 로그인실패2() throws Exception {

        //given
        emailVerificationLogsRepository.save(EmailVerificationLogs.builder().email("testEmail@gmail.com").verificationStatus(VerificationStatus.VERIFIED).expires_at(LocalDateTime.now().minusMinutes(15)).build());
        userDetailService.save(new SignupRequestDto("testEmail@gmail.com", "!1234567"));

        LoginRequestDto loginRequestDto = new LoginRequestDto("testEmail@gmail.com", "!1234567890");

        // when
        ResultActions resultActions = mockMvc.perform( // api 실행
                RestDocumentationRequestBuilders
                        .post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(loginRequestDto))
        );

        // then
        resultActions.andExpect(status().isConflict()).andExpect(jsonPath("code").value("C017")); // 상태 코드 conflict인지 확인

        // 문서 작성
        resultActions.andDo(
                document(
                        "로그인_FAIL_PASSWORD_NOT_MATCHED", // api의 id
                        resource(
                                ResourceSnippetParameters.builder()
                                        .tag("😎 AUTH-인증/인가") // 문서에서 api들이 태그로 분류됨
                                        .summary("로그인 요청 api") // api 이름
                                        .description("email&password 기반 일반 로그인 api") // api 설명
                                        .responseFields(resultDescriptors) // responseBody 설명
                                        .build()
                        )
                )
        );
    }


    @Test
    @Transactional
    @DisplayName("이메일 인증 api")
    void 이메일인증성공() throws Exception {

        //given
        EmailAuthenticationDto.EmailAuthDto emailAuthDto = new EmailAuthenticationDto.EmailAuthDto("http://localhost:8080","emailTest@gmail.com");

        // when
        ResultActions resultActions = mockMvc.perform( // api 실행
                RestDocumentationRequestBuilders
                        .put("/api/auth/email/authentication")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(emailAuthDto))
        );

        // then
        resultActions.andExpect(status().isOk()); // 상태 코드 conflict인지 확인

        // 문서 작성
        resultActions.andDo(
                document(
                        "이메일인증_SUCCESS", // api의 id
                        resource(
                                ResourceSnippetParameters.builder()
                                        .tag("😎 AUTH-인증/인가") // 문서에서 api들이 태그로 분류됨
                                        .summary("이메일 인증 요청 api") // api 이름
                                        .description("이메일 인증용 딥링크 메일 전송 & 인증 코드 생성 api") // api 설명
                                        .responseFields(resultDescriptors) // responseBody 설명
                                        .build()
                        )
                )
        );
    }

    @Test
    @Transactional
    @DisplayName("이메일 인증 api")
    void 이메일인증실패1() throws Exception {

        //given
        EmailVerificationLogs emailVerificationLogs =  EmailVerificationLogs.createLog("emailTest@gmail.com",5,"fmqwemfqwemqwegqeg" );
        emailVerificationLogsRepository.save(emailVerificationLogs);

        EmailAuthenticationDto.EmailAuthDto emailAuthDto = new EmailAuthenticationDto.EmailAuthDto("http://localhost:8080","emailTest@gmail.com");

        // when
        ResultActions resultActions = mockMvc.perform( // api 실행
                RestDocumentationRequestBuilders
                        .put("/api/auth/email/authentication")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(emailAuthDto))
        );

        // then
        resultActions.andExpect(status().isConflict()).andExpect(jsonPath("code").value("C019"));

        // 문서 작성
        resultActions.andDo(
                document(
                        "이메일인증_FAIL_EXCEED_NUMBER_OF_TRIAL_VERIFICATION", // api의 id
                        resource(
                                ResourceSnippetParameters.builder()
                                        .tag("😎 AUTH-인증/인가") // 문서에서 api들이 태그로 분류됨
                                        .summary("이메일 인증 요청 api") // api 이름
                                        .description("이메일 인증용 딥링크 메일 전송 & 인증 코드 생성 api") // api 설명
                                        .responseFields(resultDescriptors) // responseBody 설명
                                        .build()
                        )
                )
        );
    }

    @Test
    @Transactional
    @DisplayName("이메일 인증 api")
    void 이메일인증실패2() throws Exception {

        //given

        EmailVerificationLogs emailVerificationLogs =  EmailVerificationLogs.createLog("emailTest@gmail.com",1,"fmqwemfqwemqwegqeg" );
        emailVerificationLogs.updateVerificationStatus(VerificationStatus.VERIFIED);
        emailVerificationLogsRepository.save(emailVerificationLogs);

        EmailAuthenticationDto.EmailAuthDto emailAuthDto = new EmailAuthenticationDto.EmailAuthDto("http://localhost:8080","emailTest@gmail.com");

        // when
        ResultActions resultActions = mockMvc.perform( // api 실행
                RestDocumentationRequestBuilders
                        .put("/api/auth/email/authentication")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(emailAuthDto))
        );
        // then
        resultActions.andExpect(status().isConflict()).andExpect(jsonPath("code").value("C025"));

        // 문서 작성
        resultActions.andDo(
                document(
                        "이메일인증_FAIL_ALREADY_VERIFIED_EMAIL", // api의 id
                        resource(
                                ResourceSnippetParameters.builder()
                                        .tag("😎 AUTH-인증/인가") // 문서에서 api들이 태그로 분류됨
                                        .summary("이메일 인증 요청 api") // api 이름
                                        .description("이메일 인증용 딥링크 메일 전송 & 인증 코드 생성 api") // api 설명
                                        .responseFields(resultDescriptors) // responseBody 설명
                                        .build()
                        )
                )
        );
    }

    @Test
    @Transactional
    @DisplayName("이메일 인증 api")
    void 이메일인증실패3() throws Exception {

        //given
        User user = User.builder().email("emailTest@gmail.com").build();
        userRepository.save(user);

        EmailAuthenticationDto.EmailAuthDto emailAuthDto = new EmailAuthenticationDto.EmailAuthDto("http://localhost:8080","emailTest@gmail.com");

        // when
        ResultActions resultActions = mockMvc.perform( // api 실행
                RestDocumentationRequestBuilders
                        .put("/api/auth/email/authentication")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(emailAuthDto))
        );

        // then
        resultActions.andExpect(status().isConflict()).andExpect(jsonPath("code").value("C018")); ; // 상태 코드 conflict인지 확인

        // 문서 작성
        resultActions.andDo(
                document(
                        "이메일인증_FAIL_VERIFIED_USER_ALREADY_EXISTS", // api의 id
                        resource(
                                ResourceSnippetParameters.builder()
                                        .tag("😎 AUTH-인증/인가") // 문서에서 api들이 태그로 분류됨
                                        .summary("이메일 인증 요청 api") // api 이름
                                        .description("이메일 인증용 딥링크 메일 전송 & 인증 코드 생성 api") // api 설명
                                        .responseFields(resultDescriptors) // responseBody 설명
                                        .build()
                        )
                )
        );
    }

    @Test
    @Transactional
    @DisplayName("이메일 인증 api")
    void 이메일인증실패4() throws Exception {

        //given
        EmailAuthenticationDto.EmailAuthDto emailAuthDto = new EmailAuthenticationDto.EmailAuthDto("http://localhost:8080","myr@@@@naver.com");

        // when
        ResultActions resultActions = mockMvc.perform( // api 실행
                RestDocumentationRequestBuilders
                        .put("/api/auth/email/authentication")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(emailAuthDto))
        );

        // then
        resultActions.andExpect(status().isConflict()).andExpect(jsonPath("code").value("C024"));

        // 문서 작성
        resultActions.andDo(
                document(
                        "이메일인증_FAIL_INVALID_EMAIL_CONTENT", // api의 id
                        resource(
                                ResourceSnippetParameters.builder()
                                        .tag("😎 AUTH-인증/인가") // 문서에서 api들이 태그로 분류됨
                                        .summary("이메일 인증 요청 api") // api 이름
                                        .description("이메일 인증용 딥링크 메일 전송 & 인증 코드 생성 api") // api 설명
                                        .responseFields(resultDescriptors) // responseBody 설명
                                        .build()
                        )
                )
        );
    }


    @Test
    @Transactional
    @DisplayName("이메일 인증 api")
    void 이메일인증실패5() throws Exception {


        //given
        doThrow(new CustomException(ResponseCodeEnum.FAIL_TO_SEND_EMAIL,"message"))
                .when(customUserDetailService).emailAuthentication("http://localhost:8080","mygongjoo@naver.com");

        EmailAuthenticationDto.EmailAuthDto emailAuthDto = new EmailAuthenticationDto.EmailAuthDto("http://localhost:8080","mygongjoo@naver.com");

        // when
        ResultActions resultActions = mockMvc.perform( // api 실행
                RestDocumentationRequestBuilders
                        .put("/api/auth/email/authentication")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(emailAuthDto))
        );

        // then
        resultActions.andExpect(status().isInternalServerError()).andExpect(jsonPath("code").value("D002"));

        // 문서 작성
        resultActions.andDo(
                document(
                        "이메일인증_FAIL_FAIL_TO_SEND_EMAIL", // api의 id
                        resource(
                                ResourceSnippetParameters.builder()
                                        .tag("😎 AUTH-인증/인가") // 문서에서 api들이 태그로 분류됨
                                        .summary("이메일 인증 요청 api") // api 이름
                                        .description("이메일 인증용 딥링크 메일 전송 & 인증 코드 생성 api") // api 설명
                                        .responseFields(resultDescriptors) // responseBody 설명
                                        .build()
                        )
                )
        );
    }

    @Test
    @Transactional
    @DisplayName("이메일 토큰 검증 api")
    void 이메일토큰검증성공() throws Exception {

        //given

        EmailVerificationLogs emailVerificationLogs =  EmailVerificationLogs.createLog("emailTest@gmail.com",1,"testToken");
        emailVerificationLogsRepository.save(emailVerificationLogs);

        EmailAuthenticationDto.EmailTokenAuthDto emailTokenAuthDto = new EmailAuthenticationDto.EmailTokenAuthDto("testToken");

        // when
        ResultActions resultActions = mockMvc.perform( // api 실행
                RestDocumentationRequestBuilders
                        .put("/api/auth/email/token/authentication")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(emailTokenAuthDto))
        );

        // then
        resultActions.andExpect(status().isOk());

        // 문서 작성
        resultActions.andDo(
                document(
                        "이메일토큰검증_SUCCESS",
                        resource(
                                ResourceSnippetParameters.builder()
                                        .tag("😎 AUTH-인증/인가") // 문서에서 api들이 태그로 분류됨
                                        .summary("이메일 토큰 검증 요청 api") // api 이름
                                        .description("이메일 링크를 통해 제공받은 토큰을 검증하는 api") // api 설명
                                        .responseFields(resultDescriptors) // responseBody 설명
                                        .build()
                        )
                )
        );
    }

    @Test
    @Transactional
    @DisplayName("이메일 토큰 검증 api")
    void 이메일토큰검증실패1() throws Exception {

        //given
        EmailAuthenticationDto.EmailTokenAuthDto emailTokenAuthDto = new EmailAuthenticationDto.EmailTokenAuthDto("testToken");

        // when
        ResultActions resultActions = mockMvc.perform( // api 실행
                RestDocumentationRequestBuilders
                        .put("/api/auth/email/token/authentication")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(emailTokenAuthDto))
        );

        // then
        resultActions.andExpect(status().isConflict()).andExpect(jsonPath("code").value("C022"));

        // 문서 작성
        resultActions.andDo(
                document(
                        "이메일토큰검증_FAIL_EMAIL_VERIFICATION_CODE_NOT_MATCHED",
                        resource(
                                ResourceSnippetParameters.builder()
                                        .tag("😎 AUTH-인증/인가") // 문서에서 api들이 태그로 분류됨
                                        .summary("이메일 토큰 검증 요청 api") // api 이름
                                        .description("이메일 링크를 통해 제공받은 토큰을 검증하는 api") // api 설명
                                        .responseFields(resultDescriptors) // responseBody 설명
                                        .build()
                        )
                )
        );
    }

    @Test
    @Transactional
    @DisplayName("이메일 토큰 검증 api")
    void 이메일토큰검증실패2() throws Exception {

        //given

        EmailVerificationLogs emailVerificationLogs = EmailVerificationLogs.builder().email("emailTest@gmail.com").numberOfTrial(2).verificationStatus(VerificationStatus.PENDING).expires_at(LocalDateTime.now()).build();
        emailVerificationLogsRepository.save(emailVerificationLogs);
        String token= emailVerificationLogs.getVerificationToken();

        EmailAuthenticationDto.EmailTokenAuthDto emailTokenAuthDto = new EmailAuthenticationDto.EmailTokenAuthDto(token);

        // when
        ResultActions resultActions = mockMvc.perform( // api 실행
                RestDocumentationRequestBuilders
                        .put("/api/auth/email/token/authentication")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(emailTokenAuthDto))
        );

        // then
        resultActions.andExpect(status().isConflict()).andExpect(jsonPath("code").value("C021"));

        // 문서 작성
        resultActions.andDo(
                document(
                        "이메일토큰검증_FAIL_EMAIL_VERIFICATION_CODE_EXPIRED",
                        resource(
                                ResourceSnippetParameters.builder()
                                        .tag("😎 AUTH-인증/인가") // 문서에서 api들이 태그로 분류됨
                                        .summary("이메일 토큰 검증 요청 api") // api 이름
                                        .description("이메일 링크를 통해 제공받은 토큰을 검증하는 api") // api 설명
                                        .responseFields(resultDescriptors) // responseBody 설명
                                        .build()
                        )
                )
        );
    }


    @Test
    @Transactional
    @DisplayName("jwt 토큰 update api")
    void jwtTokenUpdate성공() throws Exception {

        String email = "testEmail@gmail.com";
        userRepository.save(User.builder().email(email).role(Role.GENERAL).build());

        //given
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("guest"));
        authorities.add(new SimpleGrantedAuthority("general"));

        // Authentication 객체 생성
        org.springframework.security.core.userdetails.User principal = new org.springframework.security.core.userdetails.User(email,"password",authorities);
        Authentication authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities);

        TokenDto tokenDto  = tokenProvider.createAllToken(authentication);
        refreshTokenRepository.save(RefreshToken.builder().email(email).refreshToken(tokenDto.getRefreshToken()).build());

        TokenRequestDto tokenRequestDto = new TokenRequestDto(tokenDto.getRefreshToken());

        // when
        ResultActions resultActions = mockMvc.perform( // api 실행
                RestDocumentationRequestBuilders
                        .put("/api/auth/tokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(tokenRequestDto))
        );

        // then
        resultActions.andExpect(status().isOk());

        // 문서 작성
        resultActions.andDo(
                document(
                        "jwtToken_재발급_SUCCESS",
                        resource(
                                ResourceSnippetParameters.builder()
                                        .tag("😎 AUTH-인증/인가") // 문서에서 api들이 태그로 분류됨
                                        .summary("jwt 토큰 갱신 요청 api") // api 이름
                                        .description("refresh token을 통해 access token & refresh token 모두 갱신") // api 설명
                                        .responseFields(tokenResultDescriptors) // responseBody 설명
                                        .build()
                        )
                )
        );
    }


    @Test
    @Transactional
    @DisplayName("jwt 토큰 update api")
    void jwtTokenUpdate실패1() throws Exception {

        String email = "testEmail@gmail.com";
        userRepository.save(User.builder().email(email).role(Role.GENERAL).build());

        //given
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("guest"));
        authorities.add(new SimpleGrantedAuthority("general"));

        String refreshToken = Jwts.builder()
                .setSubject(email)
                .claim("auth", authorities)
                .signWith(SignatureAlgorithm.HS512, secretKey)
                .setExpiration(new Date((new Date()).getTime()))
                .compact();
        refreshTokenRepository.save(RefreshToken.builder().email(email).refreshToken(refreshToken).build());

        TokenRequestDto tokenRequestDto = new TokenRequestDto(refreshToken);

        // when
        ResultActions resultActions = mockMvc.perform( // api 실행
                RestDocumentationRequestBuilders
                        .put("/api/auth/tokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(tokenRequestDto))
        );

        // then
        resultActions.andExpect(status().isConflict()).andExpect(jsonPath("code").value("C005"));

        // 문서 작성
        resultActions.andDo(
                document(
                        "jwtToken_재발급_FAIL_REFRESH_TOKEN_EXPIRED",
                        resource(
                                ResourceSnippetParameters.builder()
                                        .tag("😎 AUTH-인증/인가") // 문서에서 api들이 태그로 분류됨
                                        .summary("jwt 토큰 갱신 요청 api") // api 이름
                                        .description("refresh token을 통해 access token & refresh token 모두 갱신") // api 설명
                                        .responseFields(resultDescriptors) // responseBody 설명
                                        .build()
                        )
                )
        );
    }


    @Test
    @Transactional
    @DisplayName("jwt 토큰 update api")
    void jwtTokenUpdate실패2() throws Exception {

        String email = "testEmail@gmail.com";
        userRepository.save(User.builder().email(email).role(Role.GENERAL).build());

        //given
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("guest"));
        authorities.add(new SimpleGrantedAuthority("general"));

        String refreshToken = Jwts.builder()
                .setSubject(email)
                .claim("auth", authorities)
                .signWith(SignatureAlgorithm.HS512, "cb7b6812d8c462566361420d8d80e05bff34841a0558667be195a6fd92cd7b0b94d890bd8af1a6e9196e8a8491ffc63d9fd984d83e6e44b3ba2f57039552485a6aab37b43d839e11b2c7c1b63c1d58bf822b2f28cbb88477dc0aaddd358bf00f1e6171d9c81791e94e4787c3499bd71fdc1b67beed28dd29c4917ed705fe2e147df42e21b92000096615d7d9e737a7c11e7278d59dcd1f08333dc88892a7e3524fa26ae4950951e7978159411fadcd4f9102fb6e21bb721f7c298262a729da0daf9d0f229420ebadd7c6eb676180141ca2a0eeb8ba8ad0f34bfb0526942f1c8a7a8a556a924f553464a26901294ba4c05a038580c74742bbda31762f923e461910e5d21260379a778640fe9df96a00cb02fb45f433c5526a3bed95bc71787b960deef96b367a063f902085acf519960cca0acf63a136cedc26120d60a5c25a410193a793954a7afd6122503fc3692c8fd0880f32b5b2f565e50164b5a67424f9891fce3c26f2b506af525d0ea578b9b2d0f969b78f284c73d688f486cd74dbd65f106e89a78884478b639f2d0f62feb11865d5164fc29d04d339265b3b0307e1f2e916df6117890490d39cb972ca1cb325c694b2ef8dd2758e4ce2339ae772342aefd213785656477e08b0fb19dbbc28f601c8b09c85e6d648669be9f9f9c5ab69778ecab6ce3e65998b59a60e994d06476a44ff4fb9813f8c80b9b9af6217d2")
                .setExpiration(new Date((new Date()).getTime()))
                .compact();
        refreshTokenRepository.save(RefreshToken.builder().email(email).refreshToken(refreshToken).build());

        TokenRequestDto tokenRequestDto = new TokenRequestDto(refreshToken);

        // when
        ResultActions resultActions = mockMvc.perform( // api 실행
                RestDocumentationRequestBuilders
                        .put("/api/auth/tokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(tokenRequestDto))
        );

        // then
        resultActions.andExpect(status().isConflict()).andExpect(jsonPath("code").value("C009"));

        // 문서 작성
        resultActions.andDo(
                document(
                        "jwtToken_재발급_FAIL_REFRESH_TOKEN_INVALID",
                        resource(
                                ResourceSnippetParameters.builder()
                                        .tag("😎 AUTH-인증/인가") // 문서에서 api들이 태그로 분류됨
                                        .summary("jwt 토큰 갱신 요청 api") // api 이름
                                        .description("refresh token을 통해 access token & refresh token 모두 갱신") // api 설명
                                        .responseFields(resultDescriptors) // responseBody 설명
                                        .build()
                        )
                )
        );
    }

    @Test
    @Transactional
    @DisplayName("jwt 토큰 update api")
    void jwtTokenUpdate실패3() throws Exception {

        String email = "testEmail@gmail.com";

        //given
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("guest"));
        authorities.add(new SimpleGrantedAuthority("general"));

        String refreshToken = Jwts.builder()
                .setSubject(email)
                .claim("auth", authorities)
                .signWith(SignatureAlgorithm.HS512, secretKey)
                .setExpiration(new Date((new Date()).getTime()+1211))
                .compact();
        refreshTokenRepository.save(RefreshToken.builder().email(email).refreshToken(refreshToken).build());
        TokenRequestDto tokenRequestDto = new TokenRequestDto(refreshToken);

        // when
        ResultActions resultActions = mockMvc.perform( // api 실행
                RestDocumentationRequestBuilders
                        .put("/api/auth/tokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(tokenRequestDto))
        );

        // then
        resultActions.andExpect(status().isNotFound()).andExpect(jsonPath("code").value("C001"));

        // 문서 작성
        resultActions.andDo(
                document(
                        "jwtToken_재발급_FAIL_USER_NOT_FOUND",
                        resource(
                                ResourceSnippetParameters.builder()
                                        .tag("😎 AUTH-인증/인가") // 문서에서 api들이 태그로 분류됨
                                        .summary("jwt 토큰 갱신 요청 api") // api 이름
                                        .description("refresh token을 통해 access token & refresh token 모두 갱신") // api 설명
                                        .responseFields(resultDescriptors) // responseBody 설명
                                        .build()
                        )
                )
        );
    }

    @Test
    @Transactional
    @DisplayName("jwt 토큰 update api")
    void jwtTokenUpdate실패4() throws Exception {

        String email = "testEmail@gmail.com";
        userRepository.save(User.builder().email(email).role(Role.GENERAL).build());

        //given
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("guest"));
        authorities.add(new SimpleGrantedAuthority("general"));

        String refreshToken = Jwts.builder()
                .setSubject(email)
                .claim("auth", authorities)
                .signWith(SignatureAlgorithm.HS512, secretKey)
                .setExpiration(new Date((new Date()).getTime()+1211))
                .compact();
        //refreshTokenRepository.save(new RefreshToken(email,refreshToken));
        TokenRequestDto tokenRequestDto = new TokenRequestDto(refreshToken);

        // when
        ResultActions resultActions = mockMvc.perform( // api 실행
                RestDocumentationRequestBuilders
                        .put("/api/auth/tokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(tokenRequestDto))
        );

        // then
        resultActions.andExpect(status().isNotFound()).andExpect(jsonPath("code").value("C026"));

        // 문서 작성
        resultActions.andDo(
                document(
                        "jwtToken_재발급_FAIL_REFRESH_TOKEN_NOT_EXISTS",
                        resource(
                                ResourceSnippetParameters.builder()
                                        .tag("😎 AUTH-인증/인가") // 문서에서 api들이 태그로 분류됨
                                        .summary("jwt 토큰 갱신 요청 api") // api 이름
                                        .description("refresh token을 통해 access token & refresh token 모두 갱신") // api 설명
                                        .responseFields(resultDescriptors) // responseBody 설명
                                        .build()
                        )
                )
        );
    }


    @Test
    @Transactional
    @DisplayName("jwt 토큰 update api")
    void jwtTokenUpdate실패5() throws Exception {

        String email = "testEmail@gmail.com";
        userRepository.save(User.builder().email(email).role(Role.GENERAL).build());

        //given
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("guest"));
        authorities.add(new SimpleGrantedAuthority("general"));

        String refreshToken = Jwts.builder()
                .setSubject(email)
                .claim("auth", authorities)
                .signWith(SignatureAlgorithm.HS512, secretKey)
                .setExpiration(new Date((new Date()).getTime()+1211))
                .compact();
        refreshTokenRepository.save(RefreshToken.builder().email(email).refreshToken(refreshToken+"qqq").build());
        TokenRequestDto tokenRequestDto = new TokenRequestDto(refreshToken);

        // when
        ResultActions resultActions = mockMvc.perform( // api 실행
                RestDocumentationRequestBuilders
                        .put("/api/auth/tokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(tokenRequestDto))
        );

        // then
        resultActions.andExpect(status().isConflict()).andExpect(jsonPath("code").value("C007"));

        // 문서 작성
        resultActions.andDo(
                document(
                        "jwtToken_재발급_FAIL_REFRESH_TOKEN_NOT_MATCHED",
                        resource(
                                ResourceSnippetParameters.builder()
                                        .tag("😎 AUTH-인증/인가") // 문서에서 api들이 태그로 분류됨
                                        .summary("jwt 토큰 갱신 요청 api")
                                        .description("refresh token을 통해 access token & refresh token 모두 갱신") // api 설명
                                        .responseFields(resultDescriptors)
                                        .build()
                        )
                )
        );
    }



}
