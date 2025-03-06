package com.greenroom.server.api;

import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.greenroom.server.api.config.TestExecutionListener;
import com.greenroom.server.api.domain.user.entity.User;
import com.greenroom.server.api.domain.user.repository.GradeRepository;
import com.greenroom.server.api.domain.user.repository.UserRepository;
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
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import org.springframework.restdocs.operation.preprocess.HeadersModifyingOperationPreprocessor;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.restdocs.request.ParameterDescriptor;
import org.springframework.restdocs.snippet.Attributes;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@AutoConfigureMockMvc
@AutoConfigureRestDocs
@SpringBootTest
@ActiveProfiles("test")
@ExtendWith({RestDocumentationExtension.class})
@TestExecutionListeners(value = TestExecutionListener.class, mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
public class ItemIntegrationTest {

    private static final String EMAIL ="testEmail@gmail.com";
    private static final String PW = "!123456";

    private static final String NAME= "user1";

    @Value("${jwt.secret-key}")
    String secretKey;


    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GradeRepository gradeRepository;

    private final ObjectMapper mapper = new ObjectMapper();

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

    public User signupForTest(){
        User user = User.createUser(new SignupRequestDto(EMAIL,PW,NAME), gradeRepository.findById(1L).orElse(null));
        userRepository.save(user);
        return user;
    }

    private final List<FieldDescriptor> resultDescriptorsForGetItems = List.of(
            fieldWithPath("status").type(JsonFieldType.STRING).description("응답 상태")
            , fieldWithPath("code").type(JsonFieldType.STRING).description("상태 코드")
            , fieldWithPath("data").type(JsonFieldType.ARRAY).optional().description("null 또는 data")
            , fieldWithPath("data[].id").type(JsonFieldType.NUMBER).description("item 고유 id")
            , fieldWithPath("data[].name").type(JsonFieldType.STRING).description("item 이름")
            , fieldWithPath("data[].category").type(JsonFieldType.STRING).description("item category 이름")
            , fieldWithPath("data[].categoryId").type(JsonFieldType.NUMBER).description("item category id")
            , fieldWithPath("data[].subCategory").type(JsonFieldType.STRING).description("item subcategory 이름")
            , fieldWithPath("data[].subCategoryId").type(JsonFieldType.NUMBER).description("item subcategory id")
            , fieldWithPath("data[].isUnLocked").type(JsonFieldType.BOOLEAN).description("해당 item이 잠금 해제 되었는지 여부")
            , fieldWithPath("data[].unLockLevel").type(JsonFieldType.NUMBER).description("해당 item이 잠금 해제되는 레벨")
    );

    private final List<ParameterDescriptor> queryParamDescriptorForGetItems = List.of(
            parameterWithName("category").description("item 상위 분류 id").optional().attributes(new Attributes.Attribute("default","전체 상위 category")),
            parameterWithName("subcategory").description("item 하위 분류 id").optional().attributes(new Attributes.Attribute("default","전체 하위 category")));


    @Test
    @Transactional
    void 아이템_조회_성공() throws Exception {

        //given
        signupForTest();
        String token  = getTokenForTest(18000L);

        //when
        ResultActions resultActions = mockMvc.perform( // api 실행
                        RestDocumentationRequestBuilders
                                .get("/api/items")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer "+token)
                                .queryParam("category","1")
                                .queryParam("subcategory","2"));
        //then
        resultActions.andExpect(status().isOk());

        //문서화
        resultActions.andDo(document("api/items/"+1,
                preprocessRequest(prettyPrint(),modifyUris().scheme("https").host("greenroom-server.site").removePort()),   // (2)
                preprocessResponse(prettyPrint(), getModifiedHeader()),
                queryParameters(queryParamDescriptorForGetItems),
                responseFields(resultDescriptorsForGetItems), // responseBody 설명
                requestHeaders(headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer : 사용자 access Token")),
                resource(ResourceSnippetParameters.builder()
                                .tag("그린룸") // 문서에서 api들이 태그로 분류됨
                                .summary("item 조회") // api 이름
                                .description("item을 조회함.") // api 설명
                                .build())
        ));

    }
}
