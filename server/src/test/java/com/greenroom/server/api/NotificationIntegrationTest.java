package com.greenroom.server.api;

import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.greenroom.server.api.config.TestExecutionListener;
import com.greenroom.server.api.domain.greenroom.repository.GradeRepository;
import com.greenroom.server.api.domain.notification.controller.NotificationController;
import com.greenroom.server.api.domain.notification.dto.FcmTokenRequestDto;
import com.greenroom.server.api.domain.notification.dto.NotificationEnabledUpdateRequestDto;
import com.greenroom.server.api.domain.notification.entity.Notification;
import com.greenroom.server.api.domain.notification.repository.NotificationRepository;
import com.greenroom.server.api.domain.user.entity.User;
import com.greenroom.server.api.domain.user.repository.UserRepository;
import com.greenroom.server.api.enums.ResponseCodeEnum;
import com.greenroom.server.api.exception.CustomException;
import com.greenroom.server.api.security.dto.SignupRequestDto;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler;
import org.springframework.restdocs.operation.preprocess.HeadersModifyingOperationPreprocessor;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.restdocs.snippet.Attributes;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.mockito.Mockito.doThrow;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static reactor.core.publisher.Mono.when;

@Slf4j
@AutoConfigureMockMvc
@AutoConfigureRestDocs
@SpringBootTest
@ActiveProfiles("test")
@ExtendWith({RestDocumentationExtension.class})
//MERGE_WITH_DEFAULTS 옵션을 사용하면 기존의 리스너와 함께 동작 가능
@TestExecutionListeners(value = TestExecutionListener.class, mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
public class NotificationIntegrationTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    private ObjectMapper mapper;

    @Autowired
    UserRepository userRepository;

    @Autowired
    GradeRepository gradeRepository;

    @Autowired
    NotificationRepository notificationRepository;

    @MockitoSpyBean
    NotificationController notificationController;

    @Value("${jwt.secret-key}")
    String secretKey;

    @BeforeEach
    void setup(WebApplicationContext context , RestDocumentationContextProvider restDocumentation) {
        this.mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity()) //Security 필터 적용
                .apply(documentationConfiguration(restDocumentation))
                .build();
    }

    public String getTokenForTest(Long time){
        String email = "test@gmail.com";
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("GUEST"));
        authorities.add(new SimpleGrantedAuthority("GENERAL"));

        return Jwts.builder()
                .setSubject(email)
                .claim("auth", authorities)
                .signWith(SignatureAlgorithm.HS512, secretKey)
                .setExpiration(new Date(new Date().getTime()+time))
                .compact();
    }

    private User  signupForTest(){
        User user= User.createUser(new SignupRequestDto("test@gmail.com","!123456","test1"),gradeRepository.findById(1L).orElse(null));
        userRepository.save(user);
        return  user;
    }


    private HeadersModifyingOperationPreprocessor getModifiedHeader() {
        return modifyHeaders().remove("X-Content-Type-Options").remove("X-XSS-Protection").remove("Cache-Control").remove("Pragma").remove("Expires").remove("Content-Length");
    }

    private final List<FieldDescriptor> fcmTokenCreationDescriptor = List.of(
            fieldWithPath("fcmToken").type(JsonFieldType.STRING).description("user device fcm token")
    );

    private final List<FieldDescriptor> resultDescriptors = List.of(
            fieldWithPath("status").type(JsonFieldType.STRING).description("응답 상태")
            , fieldWithPath("code").type(JsonFieldType.STRING).description("상태 코드")
            , fieldWithPath("data").type(JsonFieldType.OBJECT).optional().description("null 또는 data")
    );

    private RestDocumentationResultHandler documentForFcmTokenCreation(Integer identifier){
        return document("api/notification/fcmToken/"+identifier,
                preprocessRequest(prettyPrint()),   // (2)
                preprocessResponse(prettyPrint(), getModifiedHeader()),
                responseFields(resultDescriptors), // responseBody 설명
                requestFields(fcmTokenCreationDescriptor),
                requestHeaders(
                        headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer : 사용자 access Token")
                ),
                resource(
                        ResourceSnippetParameters.builder()
                                .tag("User-회원 관련") // 문서에서 api들이 태그로 분류됨
                                .summary("fcm token 등록 api") // api 이름
                                .description("사용자의 fcm token을 등록. 이미 존재하면 갱신, 없으면 새로 등록됨.") // api 설명
                                .responseFields(resultDescriptors) // responseBody 설명
                                .requestFields(fcmTokenCreationDescriptor)
                                .requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer : 사용자 access Token"))
                                .build()));
    }

    private ResultActions resultActionsForFcmTokenCreation(FcmTokenRequestDto fcmTokenRequestDto) throws Exception {
        String accessToken = getTokenForTest((long) (60*15*1000));
        return mockMvc.perform( // api 실행
                RestDocumentationRequestBuilders
                        .post("/api/notifications/fcmToken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer "+accessToken)
                        .content(mapper.writeValueAsString(fcmTokenRequestDto)));
    }


    @Test
    @Transactional
    public void FCM_토큰생성_성공() throws Exception {

        //given
        signupForTest();
        FcmTokenRequestDto fcmTokenRequestDto = new FcmTokenRequestDto("qwerqwerqer");

        //when
        ResultActions resultActions =  resultActionsForFcmTokenCreation(fcmTokenRequestDto);

        //문서화
        resultActions.andDo(documentForFcmTokenCreation(1));

        //then
        resultActions.andExpect(status().isCreated());

    }

    //user를 찾을 수 없는 경우
    @Test
    @Transactional
    public void FCM_토큰생성_실패1() throws Exception {

        //given
        FcmTokenRequestDto fcmTokenRequestDto = new FcmTokenRequestDto("qwerqwerqer");

        //when
        ResultActions resultActions =  resultActionsForFcmTokenCreation(fcmTokenRequestDto);

        //문서화
        resultActions.andDo(documentForFcmTokenCreation(2));

        //then
        resultActions.andExpect(status().isNotFound()).andExpect(jsonPath("code").value(ResponseCodeEnum.USER_NOT_FOUND.getCode()));
    }

    //fcm token이 null 또는 ""인 경우
    @Test
    @Transactional
    public void FCM_토큰생성_실패2() throws Exception {

        //given
        signupForTest();
        FcmTokenRequestDto fcmTokenRequestDto = new FcmTokenRequestDto("");

        //when
        ResultActions resultActions =  resultActionsForFcmTokenCreation(fcmTokenRequestDto);

        //문서화
        resultActions.andDo(documentForFcmTokenCreation(3));

        //then
        resultActions.andExpect(status().isBadRequest()).andExpect(jsonPath("code").value(ResponseCodeEnum.INVALID_REQUEST_ARGUMENT.getCode()));

    }


    private RestDocumentationResultHandler documentForUpdateNotificationEnabled(Integer identifier){
        return document("api/notification/update/"+identifier,
                preprocessRequest(prettyPrint()),   // (2)
                preprocessResponse(prettyPrint(), getModifiedHeader()),
                requestFields(notificationEnabledUpdateDescriptors),
                requestHeaders(headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer : 사용자 access Token")),
                resource(
                        ResourceSnippetParameters.builder()
                                .tag("User-회원 관련") // 문서에서 api들이 태그로 분류됨
                                .summary("알림 수신 여부 변경 api") // api 이름
                                .description("푸시 알림 수신 여부를 변경") // api 설명
                                .requestFields(notificationEnabledUpdateDescriptors)
                                .requestHeaders(headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer : 사용자 access Token"))
                                .build()));
    }

    private ResultActions resultActionsForUpdateNotificationEnabled (NotificationEnabledUpdateRequestDto notificationEnabledUpdateRequestDto) throws Exception {
        String accessToken = getTokenForTest((long) (60*15*1000));
        return mockMvc.perform( // api 실행
                RestDocumentationRequestBuilders
                        .patch("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer "+accessToken)
                        .content(mapper.writeValueAsString(notificationEnabledUpdateRequestDto)));
    }

    private final List<FieldDescriptor> notificationEnabledUpdateDescriptors = List.of(
            fieldWithPath("notification_enabled").type(JsonFieldType.BOOLEAN).description("알림 수신 여부"));


    @Test
    @Transactional
    public void 알림_수신_update_성공() throws Exception {

        //given
        User user = signupForTest();
        NotificationEnabledUpdateRequestDto notificationEnabledUpdateRequestDto = new NotificationEnabledUpdateRequestDto(true);
        String accessToken = getTokenForTest((long) (60*15*1000));
        notificationRepository.save(Notification.builder().notificationEnabled(true).fcmToken("qrqrrq").user(user).build());

        //when
        ResultActions resultActions =  mockMvc.perform( // api 실행
                RestDocumentationRequestBuilders
                        .patch("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer "+accessToken)
                        .content(mapper.writeValueAsString(notificationEnabledUpdateRequestDto)));

        //문서화
        resultActions.andDo(document("api/notification/update/"+1,
                preprocessRequest(prettyPrint()),   // (2)
                preprocessResponse(prettyPrint(), getModifiedHeader()),
                requestFields(notificationEnabledUpdateDescriptors),
                requestHeaders(headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer : 사용자 access Token")),
                resource(
                        ResourceSnippetParameters.builder()
                                .tag("User-회원 관련") // 문서에서 api들이 태그로 분류됨
                                .summary("알림 수신 여부 변경 api") // api 이름
                                .description("푸시 알림 수신 여부를 변경") // api 설명
                                .requestFields(notificationEnabledUpdateDescriptors)
                                .requestHeaders(headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer : 사용자 access Token"))
                                .build())));
        //then
        resultActions.andExpect(status().isNoContent());
    }


    //user를 찾을 수 없는 경우
    @Test
    @Transactional
    public void 알림_수신_update_실패1() throws Exception {

        //when
        ResultActions resultActions = resultActionsForUpdateNotificationEnabled(new NotificationEnabledUpdateRequestDto(false));

        //문서화
        resultActions.andDo(documentForUpdateNotificationEnabled(2));

        //then
        resultActions.andExpect(status().isNotFound()).andExpect(jsonPath("code").value(ResponseCodeEnum.USER_NOT_FOUND.getCode()));

    }

    // request body의 argument가 적절하지 않은 경우 ex) null 또는 blank
    @Test
    @Transactional
    public void 알림_수신_update_실패2() throws Exception {

        //given
        signupForTest();
        org.springframework.security.core.userdetails.User user = new org.springframework.security.core.userdetails.User("test@gmail.com", "!123456", List.of(new SimpleGrantedAuthority("GUEST"),new SimpleGrantedAuthority("GENERAL")));

        doThrow(new CustomException(ResponseCodeEnum.INVALID_REQUEST_ARGUMENT)).
        when(notificationController).updateNotificationEnabled(user,new NotificationEnabledUpdateRequestDto(true));

        //when
        ResultActions resultActions = resultActionsForUpdateNotificationEnabled(new NotificationEnabledUpdateRequestDto(true));

        //문서화
        resultActions.andDo(documentForUpdateNotificationEnabled(3));

        //then
        resultActions.andExpect(status().isBadRequest()).andExpect(jsonPath("code").value(ResponseCodeEnum.INVALID_REQUEST_ARGUMENT.getCode()));
    }

    @Test
    @Transactional
    public void 알림_수신_update_실패2_1() throws Exception {


        //given
        signupForTest();

        //when
        ResultActions resultActions = resultActionsForUpdateNotificationEnabled(new NotificationEnabledUpdateRequestDto(null));

        //문서화
        resultActions.andDo(document("api/notification/update/"+3,
                preprocessRequest(prettyPrint()),   // (2)
                preprocessResponse(prettyPrint(), getModifiedHeader()),
                requestFields(fieldWithPath("notification_enabled").type(JsonFieldType.BOOLEAN).description("알림 수신 여부").optional()),
                requestHeaders(
                        headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer : 사용자 access Token")
                ),
                resource(
                        ResourceSnippetParameters.builder()
                                .tag("User-회원 관련") // 문서에서 api들이 태그로 분류됨
                                .summary("알림 수신 여부 변경 api") // api 이름
                                .description("푸시 알림 수신 여부를 변경") // api 설명
                                .requestFields(fieldWithPath("notification_enabled").type(JsonFieldType.BOOLEAN).description("알림 수신 여부").optional())
                                .requestHeaders(
                                        headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer : 사용자 access Token"))
                                .build())));
        //then
        resultActions.andExpect(status().isBadRequest()).andExpect(jsonPath("code").value(ResponseCodeEnum.INVALID_REQUEST_ARGUMENT.getCode()));
    }


    //fcm 토큰을 찾을 수 없는 경우
    @Test
    @Transactional
    public void 알림_수신_update_실패3() throws Exception {

        //given
        signupForTest();

        //when
        ResultActions resultActions = resultActionsForUpdateNotificationEnabled(new NotificationEnabledUpdateRequestDto(true));

        //문서화
        resultActions.andDo(documentForUpdateNotificationEnabled(4));

        //then
        resultActions.andExpect(status().isNotFound()).andExpect(jsonPath("code").value(ResponseCodeEnum.FCM_TOKEN_NOT_FOUND.getCode()));

    }

}
