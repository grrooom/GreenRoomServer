package com.greenroom.server.api;

import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.greenroom.server.api.config.TestDatabaseExecutionListener;
import com.greenroom.server.api.enums.ResponseCodeEnum;
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
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@AutoConfigureMockMvc
@AutoConfigureRestDocs
@SpringBootTest
@ActiveProfiles("test")
@ExtendWith({RestDocumentationExtension.class})

//테스트 클래스가 실행될 때 원하는 리스너(TestExecutionListener)를 실행할 수 있도록
@TestExecutionListeners(value = TestDatabaseExecutionListener.class, mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)

public class CommonJWTErrorTest {

    private static final String EMAIL ="testEmail@gmail.com";


    @Autowired
    private MockMvc mockMvc;

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

    private HeadersModifyingOperationPreprocessor getModifiedHeader() {
        return modifyHeaders().remove("X-Content-Type-Options").remove("X-XSS-Protection").remove("Cache-Control").remove("Pragma").remove("Expires").remove("Content-Length");
    }

    private final List<FieldDescriptor> resultDescriptors = List.of(
            fieldWithPath("status").type(JsonFieldType.STRING).description("응답 상태")
            , fieldWithPath("code").type(JsonFieldType.STRING).description("상태 코드")
            , fieldWithPath("data").type(JsonFieldType.OBJECT).optional().description("null 또는 data")
    );

    public String getTokenForTest(Long time){
        String email = EMAIL;
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("GUEST"));
        authorities.add(new SimpleGrantedAuthority("GUEST"));

        return Jwts.builder()
                .setSubject(email)
                .claim("auth", authorities)
                .signWith(SignatureAlgorithm.HS512, secretKey)
                .setExpiration(new Date(new Date().getTime()+time))
                .compact();

    }

    private RestDocumentationResultHandler documentApiForLogout(Integer identifier) {
        return document("jwt/error" + identifier
                ,
                preprocessRequest(prettyPrint()),   // (2)
                preprocessResponse(prettyPrint(), getModifiedHeader()),  // (3)
                responseFields(resultDescriptors), // responseBody 설명
                requestHeaders(
                        headerWithName("Authorization").description("Bearer : 사용자 access Token")
                ),
                resource(
                        ResourceSnippetParameters.builder()
                                .tag("응답 공통 예외") // 문서에서 api들이 태그로 분류됨
                                .summary("응답 공통 예외") // api 이름
                                .description("JWT token관련 공통 Response Error") // api 설명
                                .responseFields(resultDescriptors) // responseBody 설명
                                .requestHeaders(headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer : 사용자 access Token"))
                                .build()));
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
    public void 실패1() throws Exception {

        //when
        ResultActions resultActions = getResultActionsForLogout("");
        //then
        resultActions
                .andExpect(status().is(ResponseCodeEnum.TOKENS_NOT_FOUND.getStatus().value())).andExpect(MockMvcResultMatchers.jsonPath("code").value(ResponseCodeEnum.TOKENS_NOT_FOUND.getCode()))
                .andDo(documentApiForLogout(1));
    }

    @Test
    @Transactional
    public void 실패2() throws Exception {

        //when
        ResultActions resultActions = getResultActionsForLogout("invalidRandomToken");
        //then
        resultActions
                .andExpect(status().is(ResponseCodeEnum.ACCESS_TOKEN_INVALID.getStatus().value())).andExpect(MockMvcResultMatchers.jsonPath("code").value(ResponseCodeEnum.ACCESS_TOKEN_INVALID.getCode()))
                .andDo(documentApiForLogout(2));
    }


    @Test
    @Transactional
    public void 실패3() throws Exception {

        //given
        String token = getTokenForTest(0L);

        //when
        ResultActions resultActions = getResultActionsForLogout(token);
        //then
        resultActions
                .andExpect(status().is(ResponseCodeEnum.ACCESS_TOKEN_EXPIRED.getStatus().value())).andExpect(MockMvcResultMatchers.jsonPath("code").value(ResponseCodeEnum.ACCESS_TOKEN_EXPIRED.getCode()))
                .andDo(documentApiForLogout(3));
    }


}
