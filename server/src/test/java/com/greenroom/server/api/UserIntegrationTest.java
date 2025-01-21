package com.greenroom.server.api;

import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.greenroom.server.api.config.TestDatabaseExecutionListener;
import com.greenroom.server.api.domain.greenroom.repository.GradeRepository;
import com.greenroom.server.api.domain.user.dto.UserExitRequestDto;
import com.greenroom.server.api.domain.user.entity.User;
import com.greenroom.server.api.domain.user.repository.UserRepository;
import com.greenroom.server.api.enums.ResponseCodeEnum;
import com.greenroom.server.api.security.dto.SignupRequestDto;
import com.greenroom.server.api.security.entity.RefreshToken;
import com.greenroom.server.api.security.repository.RefreshTokenRepository;
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
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@Slf4j
@AutoConfigureMockMvc
@AutoConfigureRestDocs
@SpringBootTest
@ActiveProfiles("test")
@ExtendWith({RestDocumentationExtension.class})
@TestExecutionListeners(value = TestDatabaseExecutionListener.class, mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)

public class UserIntegrationTest {

    private static final String EMAIL ="testEmail@gmail.com";
    private static final String PW = "!123456";

    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private GradeRepository gradeRepository;

    @Value("${jwt.secret-key}")
    String secretKey;

    @BeforeEach
    void setup(WebApplicationContext context ,RestDocumentationContextProvider restDocumentation) {
        this.mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity()) //Security 필터 적용
                .apply(documentationConfiguration(restDocumentation))
                .build();
    }

    private HeadersModifyingOperationPreprocessor getModifiedHeader() {
        return modifyHeaders().remove("X-Content-Type-Options").remove("X-XSS-Protection").remove("Cache-Control").remove("Pragma").remove("Expires").remove("Content-Length");
    }

    private final List<FieldDescriptor> resultDescriptors = List.of(
            fieldWithPath("status").type(JsonFieldType.STRING).description("응답 상태")
            , fieldWithPath("code").type(JsonFieldType.STRING).description("상태 코드")
            , fieldWithPath("data").type(JsonFieldType.OBJECT).optional().description("null 또는 data")
    );

    private final List<FieldDescriptor> resultDescriptorsForExitReason = List.of(
            fieldWithPath("status").type(JsonFieldType.STRING).description("응답 상태")
            , fieldWithPath("code").type(JsonFieldType.STRING).description("상태 코드")
            , fieldWithPath("data").type(JsonFieldType.ARRAY).optional().description("null 또는 data")
            ,fieldWithPath("data[].reasonId").type(JsonFieldType.NUMBER).description("탈퇴 사유 id")
            ,fieldWithPath("data[].reason").type(JsonFieldType.STRING).description("탈퇴 사유")
    );

    private final List<FieldDescriptor> requestDescriptorsForDeactivation = List.of(
            fieldWithPath("reasonIdList").type(JsonFieldType.ARRAY).description("탈퇴 사유 id list").optional()
            , fieldWithPath("customReason").type(JsonFieldType.STRING).description("기타 탈퇴 사유").optional()
    );

    public String getTokenForTest(Long time){
        String email = EMAIL;
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


    private ResultActions getResultActionsForLogout(String token) throws Exception {
        return mockMvc.perform( // api 실행
                RestDocumentationRequestBuilders
                        .post("/api/users/logout")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer "+token)
        );
    }

    @Test
    @Transactional
    public void 로그아웃_성공() throws Exception {
        //given
        String token = getTokenForTest((long) (15*60*1000));
        User user = User.createUser(new SignupRequestDto(EMAIL,PW), gradeRepository.findById(1L).orElse(null));
        userRepository.save(user);
        refreshTokenRepository.save(RefreshToken.builder().user(user).refreshToken(token).build());

        //when
        ResultActions resultActions = getResultActionsForLogout(token);

        //then
        resultActions
                .andExpect(status().isNoContent())
                .andDo(document("api/users/logout/" + 1
                        ,
                        preprocessRequest(prettyPrint()),   // (2)
                        preprocessResponse(prettyPrint(), getModifiedHeader()),  // (3)
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer : 사용자 access Token")
                        ),
                        resource(
                                ResourceSnippetParameters.builder()
                                        .tag("User-회원 관련") // 문서에서 api들이 태그로 분류됨
                                        .summary("로그아웃 요청 api") // api 이름
                                        .description("현재 로그인된 사용자를 로그아웃 시키고, 저장된 refresh token을 무효화합니다.") // api 설명
                                        .requestHeaders(headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer : 사용자 access Token"))
                                        .build())));
    }


    @Test
    @Transactional
    public void 로그아웃_실패1() throws Exception {
        //given
        String token = getTokenForTest((long) (15*60*1000));
        //when
        ResultActions resultActions = getResultActionsForLogout(token);
        //then
        resultActions
                .andExpect(status().is(ResponseCodeEnum.USER_NOT_FOUND.getStatus().value())).andExpect(MockMvcResultMatchers.jsonPath("code").value(ResponseCodeEnum.USER_NOT_FOUND.getCode()))
                .andDo(document("api/users/logout/" + 2
                        ,
                        preprocessRequest(prettyPrint()),   // (2)
                        preprocessResponse(prettyPrint(), getModifiedHeader()),  // (3)
                        responseFields(resultDescriptors), // responseBody 설명
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer : 사용자 access Token")
                        ),
                        resource(
                                ResourceSnippetParameters.builder()
                                        .tag("User-회원 관련") // 문서에서 api들이 태그로 분류됨
                                        .summary("로그아웃 요청 api") // api 이름
                                        .description("현재 로그인된 사용자를 로그아웃 시키고, 저장된 refresh token을 무효화합니다.") // api 설명
                                        .responseFields(resultDescriptors) // responseBody 설명
                                        .requestHeaders(headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer : 사용자 access Token"))
                                        .build())));
    }

    @Test
    public void 탈퇴_사유_조회_성공() throws Exception {

        String token = getTokenForTest((long) (15*60*1000));
        ResultActions resultActions = mockMvc.perform( // api 실행
                RestDocumentationRequestBuilders
                        .get("/api/users/exitReasons")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer "+token)
        );

        resultActions
                .andExpect(status().isOk())
                .andDo(
                        document("api/users/exitReason/1" ,
                            preprocessRequest(prettyPrint()),   // (2)
                            preprocessResponse(prettyPrint(), getModifiedHeader()),  // (3)
                            responseFields(resultDescriptorsForExitReason), // responseBody 설명
                            requestHeaders(
                                headerWithName("Authorization").description("Bearer : 사용자 access Token")
                            ),
                            resource(
                                ResourceSnippetParameters.builder()
                                .tag("User-회원 관련") // 문서에서 api들이 태그로 분류됨
                                .summary("탈퇴 사유 조회 api") // api 이름
                                .description("미리 정의된 회원 탈퇴 사유를 조회합니다.") // api 설명
                                .responseFields(resultDescriptorsForExitReason) // responseBody 설명
                                .requestHeaders(headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer : 사용자 access Token"))
                                .build())));
    }

    private ResultActions getResultActionsForDeactivation(String token) throws Exception {

        UserExitRequestDto userExitRequestDto = new UserExitRequestDto(List.of(1L,2L),"그냥 마음에 들지 않음.");

        return mockMvc.perform( // api 실행
                RestDocumentationRequestBuilders
                        .delete("/api/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer "+token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(userExitRequestDto))
        );
    }

    @Test
    @Transactional
    public void 회원_탈퇴_성공() throws Exception {

        User user = User.createUser(new SignupRequestDto(EMAIL,PW), gradeRepository.findById(1L).orElse(null));
        userRepository.save(user);

        String token = getTokenForTest((long) (15*60*1000));

        ResultActions resultActions = getResultActionsForDeactivation(token);

        resultActions
                .andExpect(status().is(ResponseCodeEnum.NO_CONTENT.getStatus().value()))
                .andDo(document("api/users/delete/" + 1,
                        preprocessRequest(prettyPrint()),   // (2)
                        preprocessResponse(prettyPrint(), getModifiedHeader()),  // (3)
                        requestHeaders(
                                headerWithName("Authorization").description("Bearer : 사용자 access Token")
                        ),
                        requestFields(requestDescriptorsForDeactivation),
                        resource(
                                ResourceSnippetParameters.builder()
                                        .tag("User-회원 관련") // 문서에서 api들이 태그로 분류됨
                                        .summary("탈퇴 api") // api 이름
                                        .description("회원 탈퇴 사유를 처리하고, 회원을 삭제 대기 상태로 전환 합니다. 90일 뒤 회원과 관련한 모든 정보르 삭제합니다.") // api 설명
                                        .requestFields(requestDescriptorsForDeactivation)
                                        .requestHeaders(headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer : 사용자 access Token"))
                                        .build())
                ));
    }


    @Test
    @Transactional
    public void 회원_탈퇴_실패1() throws Exception {
        //given
        String token = getTokenForTest((long) (15*60*1000));
        //when
        ResultActions resultActions = getResultActionsForDeactivation(token);
        //then
        resultActions
                .andExpect(status().is(ResponseCodeEnum.USER_NOT_FOUND.getStatus().value())).andExpect(MockMvcResultMatchers.jsonPath("code").value(ResponseCodeEnum.USER_NOT_FOUND.getCode()))
                .andDo(document("api/users/delete/" + 2,
                        preprocessRequest(prettyPrint()),   // (2)
                        preprocessResponse(prettyPrint(), getModifiedHeader()),  // (3)
                        responseFields(resultDescriptors), // responseBody 설명
                        requestHeaders(
                                headerWithName("Authorization").description("Bearer : 사용자 access Token")
                        ),
                        requestFields(requestDescriptorsForDeactivation),
                        resource(
                                ResourceSnippetParameters.builder()
                                        .tag("User-회원 관련") // 문서에서 api들이 태그로 분류됨
                                        .summary("탈퇴 api") // api 이름
                                        .description("회원 탈퇴 사유를 처리하고, 회원을 삭제 대기 상태로 전환 합니다. 90일 뒤 회원과 관련한 모든 정보르 삭제합니다.") // api 설명
                                        .responseFields(resultDescriptors) // responseBody 설명
                                        .requestFields(requestDescriptorsForDeactivation)
                                        .requestHeaders(headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer : 사용자 access Token"))
                                        .build())
                ));
    }

}
