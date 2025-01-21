package com.greenroom.server.api;

import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.greenroom.server.api.config.TestDatabaseExecutionListener;
import com.greenroom.server.api.domain.user.entity.User;
import com.greenroom.server.api.domain.user.enums.Role;
import com.greenroom.server.api.domain.user.repository.UserRepository;
import com.greenroom.server.api.domain.user.service.UserService;
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
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler;
import org.springframework.restdocs.operation.preprocess.HeadersModifyingOperationPreprocessor;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.mockito.Mockito.*;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.snippet.Attributes.key;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@Slf4j
@AutoConfigureMockMvc
@AutoConfigureRestDocs
@SpringBootTest
@ActiveProfiles("test")
@ExtendWith({RestDocumentationExtension.class})
//MERGE_WITH_DEFAULTS 옵션을 사용하면 기존의 리스너와 함께 동작 가능
@TestExecutionListeners(value = TestDatabaseExecutionListener.class, mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
public class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

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

    @Value("${jwt.secret-key}")
    String secretKey;

    private final ObjectMapper mapper = new ObjectMapper();

    //  기본 응답 관련해서 공통 descriptor로 처리
    private final List<FieldDescriptor> resultDescriptors = List.of(
            fieldWithPath("status").type(JsonFieldType.STRING).description("응답 상태")
            , fieldWithPath("code").type(JsonFieldType.STRING).description("상태 코드")
            , fieldWithPath("data").type(JsonFieldType.OBJECT).optional().description("null 또는 data")
    );

    // request body descriptor
    private final List<FieldDescriptor> emailAndPasswordDescriptors = List.of(
            fieldWithPath("email").type(JsonFieldType.STRING).description("user email").attributes(key("constraint").value("email 형식")),
            fieldWithPath("password").type(JsonFieldType.STRING).description("user password")
    );
    private final List<FieldDescriptor> emailVerificationDescriptors = List.of(fieldWithPath("email").type(JsonFieldType.STRING).description("인증 email").attributes(key("constraint").value("email 형식")));
    private final List<FieldDescriptor> refreshTokenDescriptor = List.of(fieldWithPath("refreshToken").type(JsonFieldType.STRING).description("refresh token"));
    private final List<FieldDescriptor> emailVerificationTokenDescriptor = List.of(fieldWithPath("token").type(JsonFieldType.STRING).description("email 인증 token"));

    protected List<FieldDescriptor> tokenResultDescriptors = List.of(
            fieldWithPath("status").type(JsonFieldType.STRING).description("응답 상태")
            , fieldWithPath("code").type(JsonFieldType.STRING).description("상태 코드")
            , fieldWithPath("data").type(JsonFieldType.OBJECT).optional().description("null 또는 data")
            , fieldWithPath("data.email").type(JsonFieldType.STRING).optional().description("user email")
            , fieldWithPath("data.accessToken").type(JsonFieldType.STRING).optional().description("access token")
            , fieldWithPath("data.refreshToken").type(JsonFieldType.STRING).optional().description("refresh token")
    );

    private HeadersModifyingOperationPreprocessor getModifiedHeader() {
        return modifyHeaders().remove("X-Content-Type-Options").remove("X-XSS-Protection").remove("Cache-Control").remove("Pragma").remove("Expires").remove("Content-Length");
    }

    private RestDocumentationResultHandler documentApiForSignup(Integer identifier) {
        return document("api/auth/signup/" + identifier
                , // api의 id
                preprocessRequest(prettyPrint()),   // (2)
                preprocessResponse(prettyPrint(), getModifiedHeader()),  // (3)
                responseFields(resultDescriptors), // responseBody 설명
                requestFields(emailAndPasswordDescriptors),
                resource(
                        ResourceSnippetParameters.builder()
                                .tag("😎 AUTH-인증/인가") // 문서에서 api들이 태그로 분류됨
                                .summary("회원가입 요청 api") // api 이름
                                .description("사용자가 이메일과 비밀번호를 기반으로 새로운 계정을 생성할 수 있도록 지원합니다. \n 회원 가입 시 사용되는 사용자의 email 계정은 이메일 인증을 통한 유효성 검사를 거친 후 사용이 가능합니다. 중복 email로 가입하는 것은 제한됩니다.") // api 설명
                                .responseFields(resultDescriptors) // responseBody 설명
                                .requestFields(emailAndPasswordDescriptors)
                                .build()));
    }

    private ResultActions getResultActionsForSignup(SignupRequestDto signupRequestDto) throws Exception {
        return mockMvc.perform( // api 실행
                RestDocumentationRequestBuilders
                        .post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(signupRequestDto))
        );
    }

    @Test
    @Transactional // 테스트 완료 후 rollback
    @DisplayName("회원가입 api")
    void 회원가입성공1() throws Exception {

        //given
        emailVerificationLogsRepository.save(EmailVerificationLogs.builder().email("testEmail@gmail.com").verificationStatus(VerificationStatus.VERIFIED).verificationToken(tokenProvider.createVerificationToken("testEmail@gmail.com")).build());

        SignupRequestDto signupRequestDto = new SignupRequestDto("testEmail@gmail.com", "!123456");

        // when
        ResultActions resultActions = getResultActionsForSignup(signupRequestDto);
        // then
        resultActions.andExpect(status().isCreated()); // 상태 코드 created인지 확인
        resultActions.andDo(documentApiForSignup(1));
    }

    @Test
    @Transactional
    @DisplayName("회원가입 api")
    void 회원가입실패1() throws Exception {

        //given
        SignupRequestDto signupRequestDto = new SignupRequestDto("emailNotVerified@gmail.com", "!123456");

        // when
        ResultActions resultActions =getResultActionsForSignup(signupRequestDto);

        // then
        resultActions.andExpect(status().is(ResponseCodeEnum.EMAIL_NOT_VERIFIED.getStatus().value())).andExpect(jsonPath("code").value(ResponseCodeEnum.EMAIL_NOT_VERIFIED.getCode()));
        // 문서 작성
        resultActions.andDo(documentApiForSignup(2));
    }


    @Test
    @Transactional
    @DisplayName("회원가입 api")
    void 회원가입실패2() throws Exception {

        //given
        emailVerificationLogsRepository.save(EmailVerificationLogs.builder().email("testEmail@gmail.com").verificationStatus(VerificationStatus.VERIFIED).build());
        userDetailService.save(new SignupRequestDto("testEmail@gmail.com", "!123456"));

        SignupRequestDto signupRequestDto = new SignupRequestDto("testEmail@gmail.com", "!123456");

        // when
        ResultActions resultActions = getResultActionsForSignup(signupRequestDto);

        // then
        resultActions.andExpect(status().is(ResponseCodeEnum.USER_ALREADY_EXIST.getStatus().value())).andExpect(jsonPath("code").value(ResponseCodeEnum.USER_ALREADY_EXIST.getCode())); // 상태 코드 conflict인지 확인

        // 문서 작성
        resultActions.andDo(documentApiForSignup(3));
    }

    @Test
    @Transactional
    @DisplayName("회원가입 api")
    void 회원가입실패3() throws Exception {

        //given
        emailVerificationLogsRepository.save(EmailVerificationLogs.builder().email("testEmail@gmail.com").verificationStatus(VerificationStatus.VERIFIED).build());
        userDetailService.save(new SignupRequestDto("testEmail@gmail.com", "!123456"));

        SignupRequestDto signupRequestDto = new SignupRequestDto("testEmailgmail.com", "!123456");

        // when
        ResultActions resultActions = getResultActionsForSignup(signupRequestDto);

        // then
        resultActions.andExpect(status().is(ResponseCodeEnum.INVALID_REQUEST_ARGUMENT.getStatus().value())).andExpect(jsonPath("code").value(ResponseCodeEnum.INVALID_REQUEST_ARGUMENT.getCode()));

        // 문서 작성
        resultActions.andDo(documentApiForSignup(4));
    }

    private RestDocumentationResultHandler documentApiForLogin(Integer identifier) {
        return document(
                "api/auth/login/"+ identifier, // api의 id
                preprocessRequest(prettyPrint()),   // (2)
                preprocessResponse(prettyPrint(),getModifiedHeader()),
                responseFields(tokenResultDescriptors), // responseBody 설명
                requestFields(emailAndPasswordDescriptors),
                resource(
                        ResourceSnippetParameters.builder()
                                .tag("😎 AUTH-인증/인가") // 문서에서 api들이 태그로 분류됨
                                .summary("로그인 요청 api") // api 이름
                                .description("등록된 이메일과 비밀번호를 기반으로 사용자를 인증합니다. 인증 성공 시 Access Token과 Refresh Token을 발급하여 안전한 세션 관리를 지원합니다. \n발급된 토큰은 사용자가 애플리케이션의 보호된 리소스에 접근하는 데 사용됩니다.") // api 설명
                                .responseFields(tokenResultDescriptors) // responseBody 설명
                                .requestFields(emailAndPasswordDescriptors)
                                .build()
                )
        );
    }

    public ResultActions getResultActionsForLogin(LoginRequestDto loginRequestDto) throws Exception{
        return mockMvc.perform( // api 실행
                RestDocumentationRequestBuilders
                        .post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(loginRequestDto))
        );
    }

    @Test
    @Transactional
    @DisplayName("로그인 api")
    void 로그인성공() throws Exception {

        //given
        emailVerificationLogsRepository.save(EmailVerificationLogs.builder().email("testEmail@gmail.com").verificationStatus(VerificationStatus.VERIFIED).build());
        userDetailService.save(new SignupRequestDto("testEmail@gmail.com", "!123456"));

        LoginRequestDto loginRequestDto = new LoginRequestDto("testEmail@gmail.com", "!123456");

        // when
        ResultActions resultActions = getResultActionsForLogin(loginRequestDto);

        // then
        resultActions.andExpect(status().isOk()); // 상태 코드 success인지 확인

        // 문서 작성
        resultActions.andDo(documentApiForLogin(1));
    }

    @Test
    @Transactional
    @DisplayName("로그인 api")
    void 로그인실패1() throws Exception {

        //given
        LoginRequestDto loginRequestDto = new LoginRequestDto("testEmail@gmail.com", "!123456");

        // when
        ResultActions resultActions = getResultActionsForLogin(loginRequestDto);
        // then
        resultActions.andExpect(status().is(ResponseCodeEnum.USER_NOT_FOUND.getStatus().value())).andExpect(jsonPath("code").value(ResponseCodeEnum.USER_NOT_FOUND.getCode())); // 상태 코드 not found인지 확인

        // 문서 작성
        resultActions.andDo(documentApiForLogin(2));
    }

    @Test
    @Transactional
    @DisplayName("로그인 api")
    void 로그인실패2() throws Exception {

        //given
        emailVerificationLogsRepository.save(EmailVerificationLogs.builder().email("testEmail@gmail.com").verificationStatus(VerificationStatus.VERIFIED).build());
        userDetailService.save(new SignupRequestDto("testEmail@gmail.com", "!1234567"));

        LoginRequestDto loginRequestDto = new LoginRequestDto("testEmail@gmail.com", "!1234567890");

        // when
        ResultActions resultActions = getResultActionsForLogin(loginRequestDto);

        // then
        resultActions.andExpect(status().is(ResponseCodeEnum.PASSWORD_NOT_MATCHED.getStatus().value())).andExpect(jsonPath("code").value(ResponseCodeEnum.PASSWORD_NOT_MATCHED.getCode())); // 상태 코드 conflict인지 확인

        // 문서 작성
        resultActions.andDo(documentApiForLogin(3));
    }

    @Test
    @Transactional
    @DisplayName("로그인 api")
    void 로그인실패3() throws Exception {

        //given
        LoginRequestDto loginRequestDto = new LoginRequestDto("", "!1234567890");

        // when
        ResultActions resultActions = getResultActionsForLogin(loginRequestDto);

        // then
        resultActions.andExpect(status().is(ResponseCodeEnum.INVALID_REQUEST_ARGUMENT.getStatus().value())).andExpect(jsonPath("code").value(ResponseCodeEnum.INVALID_REQUEST_ARGUMENT.getCode())); // 상태 코드 conflict인지 확인

        // 문서 작성
        resultActions.andDo(documentApiForLogin(4));
    }

    private RestDocumentationResultHandler documentApiForEmailAuth(Integer identifier){
        return document(
                "api/auth/email/authentication/"+ identifier, // api의 id
                preprocessRequest(prettyPrint()),   // (2)
                preprocessResponse(prettyPrint(),getModifiedHeader()),
                responseFields(resultDescriptors), // responseBody 설명
                requestFields(emailVerificationDescriptors),
                resource(
                        ResourceSnippetParameters.builder()
                                .tag("😎 AUTH-인증/인가") // 문서에서 api들이 태그로 분류됨
                                .summary("이메일 인증 요청 api") // api 이름
                                .description("회원가입 이전에 사용자의 이메일 주소를 검증하기 위해 사용됩니다. 이 API를 요청하면 이메일을 인증하기 위한 jwt 토큰을 앱링크와 함께 전송합니다. \n5회를 초과하여 인증을 시도할 경우 15분간 추가적인 시도가 제한됩니다. 이미 가입된 user의 email 또는 이미 인증이 완료된 email에 대해서는 추가적인 인증을 제한합니다.") // api 설명
                                .responseFields(resultDescriptors) // responseBody 설명
                                .requestFields(emailVerificationDescriptors)
                                .build()
                )
        );
    }

    private ResultActions getResultActionsForEmailAuth(EmailAuthenticationDto.EmailAuthDto emailAuthDto) throws Exception{
        return mockMvc.perform( // api 실행
                RestDocumentationRequestBuilders
                        .put("/api/auth/email/authentication")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(emailAuthDto))
        );
    }

    @Test
    @Transactional
    @DisplayName("이메일 인증 api")
    void 이메일인증성공() throws Exception {

        //given
        EmailAuthenticationDto.EmailAuthDto emailAuthDto = new EmailAuthenticationDto.EmailAuthDto("emailTest@gmail.com");

        // when
        ResultActions resultActions = getResultActionsForEmailAuth(emailAuthDto);

        // then
        resultActions.andExpect(status().isNoContent());

        // 문서 작성
        resultActions.andDo(document(
                "api/auth/email/authentication/"+ 1, // api의 id
                preprocessRequest(prettyPrint()),   // (2)
                preprocessResponse(prettyPrint(),getModifiedHeader()),
                requestFields(emailVerificationDescriptors),
                resource(
                        ResourceSnippetParameters.builder()
                                .tag("😎 AUTH-인증/인가") // 문서에서 api들이 태그로 분류됨
                                .summary("이메일 인증 요청 api") // api 이름
                                .description("회원가입 이전에 사용자의 이메일 주소를 검증하기 위해 사용됩니다. 이 API를 요청하면 이메일을 인증하기 위한 jwt 토큰을 앱링크와 함께 전송합니다. \n5회를 초과하여 인증을 시도할 경우 15분간 추가적인 시도가 제한됩니다. 이미 가입된 user의 email 또는 이미 인증이 완료된 email에 대해서는 추가적인 인증을 제한합니다.") // api 설명
                                .requestFields(emailVerificationDescriptors)
                                .build()
                )
        ));
    }

    @Test
    @DisplayName("이메일 인증 api")
    @Transactional
    void 이메일인증실패1() throws Exception {

        //given
        String email = "testEmail@gmail.com";
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("guest"));
        authorities.add(new SimpleGrantedAuthority("general"));

        String token = Jwts.builder()
                .setSubject(email)
                .claim("auth", authorities)
                .signWith(SignatureAlgorithm.HS512, secretKey)
                .setExpiration(new Date(new Date().getTime()+15*60*1000))
                .compact();
        EmailVerificationLogs emailVerificationLogs =  EmailVerificationLogs.builder().email(email).numberOfTrial(5).verificationToken(token).verificationStatus(VerificationStatus.PENDING).build();
        emailVerificationLogs.setUpdateDate(LocalDateTime.now());
        emailVerificationLogsRepository.save(emailVerificationLogs);

        EmailAuthenticationDto.EmailAuthDto emailAuthDto = new EmailAuthenticationDto.EmailAuthDto(email);

        // when
        ResultActions resultActions = getResultActionsForEmailAuth(emailAuthDto);

        // then
        resultActions.andExpect(status().is(ResponseCodeEnum.EXCEED_NUMBER_OF_TRIAL_VERIFICATION.getStatus().value())).andExpect(jsonPath("code").value(ResponseCodeEnum.EXCEED_NUMBER_OF_TRIAL_VERIFICATION.getCode()));

        // 문서 작성
        resultActions.andDo(documentApiForEmailAuth(2));
    }

    @Test
    @Transactional
    @DisplayName("이메일 인증 api")
    void 이메일인증실패2() throws Exception {

        //given

        EmailVerificationLogs emailVerificationLogs =  EmailVerificationLogs.createLog("emailTest@gmail.com","fmqwemfqwemqwegqeg" );
        emailVerificationLogs.updateVerificationStatus(VerificationStatus.VERIFIED);
        emailVerificationLogsRepository.save(emailVerificationLogs);

        EmailAuthenticationDto.EmailAuthDto emailAuthDto = new EmailAuthenticationDto.EmailAuthDto("emailTest@gmail.com");

        // when
        ResultActions resultActions = getResultActionsForEmailAuth(emailAuthDto);
        // then
        resultActions.andExpect(status().is(ResponseCodeEnum.ALREADY_VERIFIED_EMAIL.getStatus().value())).andExpect(jsonPath("code").value(ResponseCodeEnum.ALREADY_VERIFIED_EMAIL.getCode()));

        // 문서 작성
        resultActions.andDo(documentApiForEmailAuth(3));
    }

    @Test
    @Transactional
    @DisplayName("이메일 인증 api")
    void 이메일인증실패3() throws Exception {

        //given
        User user = User.builder().email("emailTest@gmail.com").build();
        userRepository.save(user);

        EmailAuthenticationDto.EmailAuthDto emailAuthDto = new EmailAuthenticationDto.EmailAuthDto("emailTest@gmail.com");

        // when
        ResultActions resultActions = getResultActionsForEmailAuth(emailAuthDto);

        // then
        resultActions.andExpect(status().is(ResponseCodeEnum.VERIFIED_USER_ALREADY_EXISTS.getStatus().value())).andExpect(jsonPath("code").value(ResponseCodeEnum.VERIFIED_USER_ALREADY_EXISTS.getCode())); ; // 상태 코드 conflict인지 확인

        // 문서 작성
        resultActions.andDo(documentApiForEmailAuth(4));
    }

    @Test
    @Transactional
    @DisplayName("이메일 인증 api")
    void 이메일인증실패4() throws Exception {

        //given
        EmailAuthenticationDto.EmailAuthDto emailAuthDto = new EmailAuthenticationDto.EmailAuthDto("testEmail@gmail.com");

        doThrow(new CustomException(ResponseCodeEnum.EMAIL_ADDRESS_UNAVAILABLE,"message")).
            when(customUserDetailService).emailAuthentication("testEmail@gmail.com");
        // when
        ResultActions resultActions =getResultActionsForEmailAuth(emailAuthDto);
        // then
        resultActions.andExpect(status().is(ResponseCodeEnum.EMAIL_ADDRESS_UNAVAILABLE.getStatus().value())).andExpect(jsonPath("code").value(ResponseCodeEnum.EMAIL_ADDRESS_UNAVAILABLE.getCode()));
        // 문서 작성
        resultActions.andDo(documentApiForEmailAuth(5));
    }


    @Test
    @Transactional
    @DisplayName("이메일 인증 api")
    void 이메일인증실패5() throws Exception {


        //given
        doThrow(new CustomException(ResponseCodeEnum.FAIL_TO_SEND_EMAIL,"message"))
                .when(customUserDetailService).emailAuthentication("mygongjoo@naver.com");

        EmailAuthenticationDto.EmailAuthDto emailAuthDto = new EmailAuthenticationDto.EmailAuthDto("mygongjoo@naver.com");
        // when
        ResultActions resultActions = getResultActionsForEmailAuth(emailAuthDto);
        // then
        resultActions.andExpect(status().is(ResponseCodeEnum.FAIL_TO_SEND_EMAIL.getStatus().value())).andExpect(jsonPath("code").value(ResponseCodeEnum.FAIL_TO_SEND_EMAIL.getCode()));
        // 문서 작성
        resultActions.andDo(documentApiForEmailAuth(6));
    }

    @Test
    @Transactional
    @DisplayName("이메일 인증 api")
    void 이메일인증실패6() throws Exception {

        //given
        EmailAuthenticationDto.EmailAuthDto emailAuthDto = new EmailAuthenticationDto.EmailAuthDto("testEmail.com");
        // when
        ResultActions resultActions = getResultActionsForEmailAuth(emailAuthDto);
        // then
        resultActions.andExpect(status().is(ResponseCodeEnum.INVALID_REQUEST_ARGUMENT.getStatus().value())).andExpect(jsonPath("code").value(ResponseCodeEnum.INVALID_REQUEST_ARGUMENT.getCode()));
        // 문서 작성
        resultActions.andDo(documentApiForEmailAuth(7));
    }


    private RestDocumentationResultHandler documentApiForEmailToken(Integer identifier){
        return document(
                "api/auth/email/token/authentication/"+ identifier,
                preprocessRequest(prettyPrint()),   // (2)
                preprocessResponse(prettyPrint(),getModifiedHeader()),
                responseFields(resultDescriptors), // responseBody 설명
                requestFields(emailVerificationTokenDescriptor),
                resource(
                        ResourceSnippetParameters.builder()
                                .tag("😎 AUTH-인증/인가") // 문서에서 api들이 태그로 분류됨
                                .summary("이메일 토큰 검증 요청 api") // api 이름
                                .description("이메일 인증 요청 시 전송된 JWT 토큰을 검증하여, 해당 토큰이 유효한 경우 이메일 인증을 완료하고, 잘못되거나 만료된 토큰을 전송한 경우 인증을 거부합니다. \nJWT 토큰은 15분의 유효 시간을 가지며 가장 마지막 인증 시도 시 전달된 JWT 토큰만 유효성을 가집니다. ") // api 설명
                                .responseFields(resultDescriptors) // responseBody 설명
                                .requestFields(emailVerificationTokenDescriptor)
                                .build()
                )
        );
    }

    private ResultActions getResultActionsForEmailToken(EmailAuthenticationDto.EmailTokenAuthDto emailTokenAuthDto) throws Exception {
        return mockMvc.perform( // api 실행
                RestDocumentationRequestBuilders
                        .put("/api/auth/email/token/authentication")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(emailTokenAuthDto))
        );
    }

    @Test
    @Transactional
    @DisplayName("이메일 토큰 검증 api")
    void 이메일토큰검증성공() throws Exception {

        //given
        String email = "testEmail@gmail.com";
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("guest"));
        authorities.add(new SimpleGrantedAuthority("general"));

        String token = Jwts.builder()
                .setSubject(email)
                .claim("auth", authorities)
                .signWith(SignatureAlgorithm.HS512, secretKey)
                .setExpiration(new Date((new Date()).getTime()+600*1000))
                .compact();

        EmailVerificationLogs emailVerificationLogs =  EmailVerificationLogs.createLog("testEmail@gmail.com",token);
        emailVerificationLogsRepository.save(emailVerificationLogs);

        EmailAuthenticationDto.EmailTokenAuthDto emailTokenAuthDto = new EmailAuthenticationDto.EmailTokenAuthDto(token);

        // when
        ResultActions resultActions = getResultActionsForEmailToken(emailTokenAuthDto);

        // then
        resultActions.andExpect(status().isNoContent());

        // 문서 작성
        resultActions.andDo(document(
                "api/auth/email/token/authentication/"+ 1,
                preprocessRequest(prettyPrint()),   // (2)
                preprocessResponse(prettyPrint(),getModifiedHeader()),
                requestFields(emailVerificationTokenDescriptor),
                resource(
                        ResourceSnippetParameters.builder()
                                .tag("😎 AUTH-인증/인가") // 문서에서 api들이 태그로 분류됨
                                .summary("이메일 토큰 검증 요청 api") // api 이름
                                .description("이메일 인증 요청 시 전송된 JWT 토큰을 검증하여, 해당 토큰이 유효한 경우 이메일 인증을 완료하고, 잘못되거나 만료된 토큰을 전송한 경우 인증을 거부합니다. \nJWT 토큰은 15분의 유효 시간을 가지며 가장 마지막 인증 시도 시 전달된 JWT 토큰만 유효성을 가집니다. ") // api 설명
                                .requestFields(emailVerificationTokenDescriptor)
                                .build()
                )
        ));
    }

    @Test
    @Transactional
    @DisplayName("이메일 토큰 검증 api")
    void 이메일토큰검증실패1() throws Exception {

        //given
        EmailAuthenticationDto.EmailTokenAuthDto emailTokenAuthDto = new EmailAuthenticationDto.EmailTokenAuthDto("testToken");

        // when
        ResultActions resultActions = getResultActionsForEmailToken(emailTokenAuthDto);

        // then
        resultActions.andExpect(status().is(ResponseCodeEnum.EMAIL_VERIFICATION_CODE_NOT_MATCHED.getStatus().value())).andExpect(jsonPath("code").value(ResponseCodeEnum.EMAIL_VERIFICATION_CODE_NOT_MATCHED.getCode()));

        // 문서 작성
        resultActions.andDo(documentApiForEmailToken(2));
    }

    @Test
    @Transactional
    @DisplayName("이메일 토큰 검증 api")
    void 이메일토큰검증실패2() throws Exception {

        //given
        String email = "testEmail@gmail.com";
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("guest"));
        authorities.add(new SimpleGrantedAuthority("general"));

        String token = Jwts.builder()
                .setSubject(email)
                .claim("auth", authorities)
                .signWith(SignatureAlgorithm.HS512, secretKey)
                .setExpiration(new Date((new Date()).getTime()))
                .compact();

        EmailAuthenticationDto.EmailTokenAuthDto emailTokenAuthDto = new EmailAuthenticationDto.EmailTokenAuthDto(token);

        // when
        ResultActions resultActions = getResultActionsForEmailToken(emailTokenAuthDto);
        // then
        resultActions.andExpect(status().is(ResponseCodeEnum.EMAIL_VERIFICATION_CODE_EXPIRED.getStatus().value())).andExpect(jsonPath("code").value(ResponseCodeEnum.EMAIL_VERIFICATION_CODE_EXPIRED.getCode()));
        // 문서 작성
        resultActions.andDo(documentApiForEmailToken(3));
    }


    @Test
    @Transactional
    @DisplayName("이메일 토큰 검증 api")
    void 이메일토큰검증실패3() throws Exception {

        //given

        EmailVerificationLogs emailVerificationLogs = EmailVerificationLogs.builder().email("emailTest@gmail.com").numberOfTrial(2).verificationStatus(VerificationStatus.PENDING).build();
        emailVerificationLogsRepository.save(emailVerificationLogs);


        EmailAuthenticationDto.EmailTokenAuthDto emailTokenAuthDto = new EmailAuthenticationDto.EmailTokenAuthDto("");

        // when
        ResultActions resultActions = getResultActionsForEmailToken(emailTokenAuthDto);

        // then
        resultActions.andExpect(status().is(ResponseCodeEnum.INVALID_REQUEST_ARGUMENT.getStatus().value())).andExpect(jsonPath("code").value(ResponseCodeEnum.INVALID_REQUEST_ARGUMENT.getCode()));

        // 문서 작성
        resultActions.andDo(documentApiForEmailToken(4));
    }


    private ResultActions getResultActionsForTokenUpdate(TokenRequestDto tokenRequestDto) throws Exception {
        return mockMvc.perform( // api 실행
                RestDocumentationRequestBuilders
                        .put("/api/auth/tokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(tokenRequestDto))
        );
    }

    private RestDocumentationResultHandler documentApiForTokenUpdate(Integer identifier){
        return document(
                "api/auth/tokens/"+ identifier,
                preprocessRequest(prettyPrint()),   // (2)
                preprocessResponse(prettyPrint(),getModifiedHeader()),
                responseFields(tokenResultDescriptors), // responseBody 설명
                requestFields(refreshTokenDescriptor),
                resource(
                        ResourceSnippetParameters.builder()
                                .tag("😎 AUTH-인증/인가") // 문서에서 api들이 태그로 분류됨
                                .summary("jwt 토큰 갱신 요청 api") // api 이름
                                .description("사용자가 보유한 Refresh Token을 기반으로 새로운 Access Token과 Refresh Token을 발급합니다.\n" +
                                        "보안 강화를 위해 Refresh Token의 유효성을 검사하며, 서버에서 발급한 가장 최신의 토큰인지 확인합니다.\n" +
                                        "유효한 Refresh Token이 제공되면 새롭게 갱신된 토큰 세트를 반환합니다.\n" +
                                        "잘못된 토큰 혹은 만료되었거나 무효화된 토큰이 제공되면 에러를 반환합니다.\n" +
                                        "\n") // api 설명
                                .responseFields(tokenResultDescriptors) // responseBody 설명
                                .requestFields(refreshTokenDescriptor)
                                .build()
                )
        );
    }

    @Test
    @Transactional
    @DisplayName("jwt 토큰 update api")
    void jwtTokenUpdate성공() throws Exception {

        String email = "testEmail@gmail.com";
        User user = User.builder().email(email).role(Role.GENERAL).build();
        userRepository.save(user);

        //given
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("guest"));
        authorities.add(new SimpleGrantedAuthority("general"));

        // Authentication 객체 생성
        org.springframework.security.core.userdetails.User principal = new org.springframework.security.core.userdetails.User(email,"password",authorities);
        Authentication authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities);

        TokenDto tokenDto  = tokenProvider.createAllToken(authentication);
        refreshTokenRepository.save(RefreshToken.createRefreshToken(user, tokenDto.getRefreshToken()));

        TokenRequestDto tokenRequestDto = new TokenRequestDto(tokenDto.getRefreshToken());

        // when
        ResultActions resultActions = getResultActionsForTokenUpdate(tokenRequestDto);

        // then
        resultActions.andExpect(status().isOk());

        // 문서 작성
        resultActions.andDo(documentApiForTokenUpdate(1));
    }

    @Test
    @Transactional
    @DisplayName("jwt 토큰 update api")
    void jwtTokenUpdate실패1() throws Exception {

        String email = "testEmail@gmail.com";
        User user= User.builder().email(email).role(Role.GENERAL).build();
        userRepository.save(user);

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
        refreshTokenRepository.save(RefreshToken.createRefreshToken(user,refreshToken));

        TokenRequestDto tokenRequestDto = new TokenRequestDto(refreshToken);

        // when
        ResultActions resultActions = getResultActionsForTokenUpdate(tokenRequestDto);

        // then
        resultActions.andExpect(status().is(ResponseCodeEnum.REFRESH_TOKEN_EXPIRED.getStatus().value())).andExpect(jsonPath("code").value(ResponseCodeEnum.REFRESH_TOKEN_EXPIRED.getCode()));

        // 문서 작성
        resultActions.andDo(documentApiForTokenUpdate(2));
    }

    @Test
    @Transactional
    @DisplayName("jwt 토큰 update api")
    void jwtTokenUpdate실패2() throws Exception {

        //given
        String email = "testEmail@gmail.com";
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("guest"));
        authorities.add(new SimpleGrantedAuthority("general"));

        String refreshToken = Jwts.builder()
                .setSubject(email)
                .claim("auth", authorities)
                .signWith(SignatureAlgorithm.HS512, secretKey)
                .setExpiration(new Date((new Date()).getTime()+1211))
                .compact();

        TokenRequestDto tokenRequestDto = new TokenRequestDto(refreshToken);

        // when
        ResultActions resultActions = getResultActionsForTokenUpdate(tokenRequestDto);

        // then
        resultActions.andExpect(status().is(ResponseCodeEnum.USER_NOT_FOUND.getStatus().value())).andExpect(jsonPath("code").value(ResponseCodeEnum.USER_NOT_FOUND.getCode()));

        // 문서 작성
        resultActions.andDo(documentApiForTokenUpdate(3));
    }

    @Test
    @Transactional
    @DisplayName("jwt 토큰 update api")
    void jwtTokenUpdate실패4() throws Exception {

        String email = "testEmail@gmail.com";
        User user = User.builder().email(email).role(Role.GENERAL).build();
        userRepository.save(user);

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

        refreshTokenRepository.save(RefreshToken.createRefreshToken(user,refreshToken+"qq"));
        TokenRequestDto tokenRequestDto = new TokenRequestDto(refreshToken);

        // when
        ResultActions resultActions = getResultActionsForTokenUpdate(tokenRequestDto);

        // then
        resultActions.andExpect(status().is(ResponseCodeEnum.REFRESH_TOKEN_NOT_MATCHED.getStatus().value())).andExpect(jsonPath("code").value(ResponseCodeEnum.REFRESH_TOKEN_NOT_MATCHED.getCode()));

        // 문서 작성
        resultActions.andDo(documentApiForTokenUpdate(4));
    }


    @Test
    @Transactional
    @DisplayName("jwt 토큰 update api")
    void jwtTokenUpdate실패5() throws Exception {

        //given
        TokenRequestDto tokenRequestDto = new TokenRequestDto("");

        // when
        ResultActions resultActions = getResultActionsForTokenUpdate(tokenRequestDto);

        // then
        resultActions.andExpect(status().is(ResponseCodeEnum.INVALID_REQUEST_ARGUMENT.getStatus().value())).andExpect(jsonPath("code").value(ResponseCodeEnum.INVALID_REQUEST_ARGUMENT.getCode()));

        // 문서 작성
        resultActions.andDo(documentApiForTokenUpdate(5));
    }


}
