package com.greenroom.server.api;


import com.epages.restdocs.apispec.ResourceSnippetParameters;

import static org.mockito.Mockito.doReturn;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.greenroom.server.api.config.TestExecutionListener;
import com.greenroom.server.api.domain.greenroom.document.PlantDocument;
import com.greenroom.server.api.domain.greenroom.dto.in.ActivityInfoUpdateRequestDto;
import com.greenroom.server.api.domain.greenroom.dto.in.GreenroomPlantRequestDto;
import com.greenroom.server.api.domain.greenroom.entity.GreenRoom;
import com.greenroom.server.api.domain.greenroom.repository.*;
import com.greenroom.server.api.domain.greenroom.service.PlantService;
import com.greenroom.server.api.domain.user.entity.User;
import com.greenroom.server.api.domain.user.repository.GradeRepository;
import com.greenroom.server.api.domain.user.repository.UserRepository;
import com.greenroom.server.api.global.response.enums.ResponseCodeEnum;
import com.greenroom.server.api.global.exception.CustomException;
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
import org.springframework.restdocs.request.ParameterDescriptor;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.mockito.Mockito.doThrow;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static reactor.core.publisher.Mono.when;

@Slf4j
@AutoConfigureMockMvc
@AutoConfigureRestDocs
@SpringBootTest
@ActiveProfiles("test")
@ExtendWith({RestDocumentationExtension.class})
@TestExecutionListeners(value = TestExecutionListener.class, mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
public class PlantIntegrationTest {

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

    @Autowired
    private PlantDocumentRepository plantDocumentRepository;

    @Autowired
    private PlantRepository plantRepository;

    @MockitoSpyBean
    private PlantService plantMockitoService;





    private final ObjectMapper mapper = new ObjectMapper();

    //  기본 응답 관련해서 공통 descriptor로 처리
    private final List<FieldDescriptor> resultDescriptors = List.of(
            fieldWithPath("status").type(JsonFieldType.STRING).description("응답 상태")
            , fieldWithPath("code").type(JsonFieldType.STRING).description("상태 코드")
            , fieldWithPath("data").type(JsonFieldType.OBJECT).optional().description("data")
    );

    @BeforeEach
    void setup(WebApplicationContext context , RestDocumentationContextProvider restDocumentation) {
        this.mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity()) //Security 필터 적용
                .apply(documentationConfiguration(restDocumentation))
                .build();
    }


    public void setElsData() {
        plantDocumentRepository.saveAll(List.of(new PlantDocument(261L,"드라세나 자바2","dracena java","https://dkvqbjpz8l4h9.cloudfront.net/plant/14696_MF_ATTACH_02.jpg"),
                new PlantDocument(691L,"드라세나 드라코2","dracena draco","https://dkvqbjpz8l4h9.cloudfront.net/plant/14688_MF_ATTACH_02.jpg")));
    }

    public void cleanupElsData(){
        plantDocumentRepository.deleteAll();
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

    private HeadersModifyingOperationPreprocessor getModifiedHeader() {
        return modifyHeaders().remove("X-Content-Type-Options").remove("X-XSS-Protection").remove("Cache-Control").remove("Pragma").remove("Expires").remove("Content-Length");
    }

    private final List<FieldDescriptor> resultDescriptorsForPlantsList = List.of(
            fieldWithPath("status").type(JsonFieldType.STRING).description("응답 상태"),
            fieldWithPath("code").type(JsonFieldType.STRING).description("상태 코드"),
            fieldWithPath("data").type(JsonFieldType.ARRAY).optional().description("data").attributes(new Attributes.Attribute("constraint","결과값이 없으면 빈 배열 반환")),
            fieldWithPath("data[].plantId").type(JsonFieldType.NUMBER).description("식물 id"),
            fieldWithPath("data[].plantName").type(JsonFieldType.STRING).description("식물 이름"),
            fieldWithPath("data[].imageUrl").type(JsonFieldType.STRING).description("식물 이미지 url")
            );

    private final List<ParameterDescriptor> queryParametersForPlantSearch = List.of(
            parameterWithName("keyword").description("식물 검색 키워드").optional().attributes(new Attributes.Attribute("default","\" \" 공백으로 간주")),
            parameterWithName("size").description("응답에서 반환할 데이터의 최대 개수").optional().attributes(new Attributes.Attribute("default","전체 결과 반환"))
    );

    private final List<ParameterDescriptor> queryParametersForSize = List.of(
            parameterWithName("size").description("응답에서 반환할 데이터의 최대 개수").optional().attributes(new Attributes.Attribute("default","전체 결과 반환"))
    );

    private final List<ParameterDescriptor> pathParametersForPlantId = List.of(
            parameterWithName("plantId").description("식물 id")
    );

    private final List<FieldDescriptor> resultDescriptorsForPlantWateringInfo = List.of(
            fieldWithPath("status").type(JsonFieldType.STRING).description("응답 상태"),
            fieldWithPath("code").type(JsonFieldType.STRING).description("상태 코드"),
            fieldWithPath("data").type(JsonFieldType.OBJECT).optional().description("data"),
            fieldWithPath("data.plantId").type(JsonFieldType.NUMBER).description("식물 id"),
            fieldWithPath("data.plantName").type(JsonFieldType.STRING).description("식물 이름"),
            fieldWithPath("data.wateringInfo").type(JsonFieldType.STRING).description("식물 물주기 정보")
    );

    List<FieldDescriptor> resultDescriptorsForPlantDetails= List.of(
            fieldWithPath("status").type(JsonFieldType.STRING).description("응답 상태"),
            fieldWithPath("code").type(JsonFieldType.STRING).description("상태 코드"),
            fieldWithPath("data").type(JsonFieldType.OBJECT).optional().description("data").optional(),
            fieldWithPath("data.plantInfo").type(JsonFieldType.OBJECT).description("식물 기본 정보"),
            fieldWithPath("data.plantInfo.plantId").type(JsonFieldType.NUMBER).description("식물 id").optional(),
            fieldWithPath("data.plantInfo.name").type(JsonFieldType.STRING).description("식물 이름").optional(),
            fieldWithPath("data.plantInfo.scientificName").type(JsonFieldType.STRING).description("식물 학명").optional(),
            fieldWithPath("data.plantInfo.description").type(JsonFieldType.STRING).description("식물에 대한 설명").optional(),
            fieldWithPath("data.plantInfo.imageUrl").type(JsonFieldType.STRING).description("식물 사진").optional(),
            fieldWithPath("data.plantManagementInfo").type(JsonFieldType.OBJECT).description("식물 키우는 법"),
            fieldWithPath("data.plantManagementInfo.managementLevel").type(JsonFieldType.STRING).description("관리 레벨 정보").optional(),
            fieldWithPath("data.plantManagementInfo.temperature").type(JsonFieldType.STRING).description("온도 정보").optional(),
            fieldWithPath("data.plantManagementInfo.sunlight").type(JsonFieldType.STRING).description("햇빛 정보").optional(),
            fieldWithPath("data.plantManagementInfo.watering").type(JsonFieldType.STRING).description("물주기 정보").optional(),
            fieldWithPath("data.plantManagementInfo.humidity").type(JsonFieldType.STRING).description("습도 정보").optional(),
            fieldWithPath("data.plantManagementInfo.fertilizer").type(JsonFieldType.STRING).description("비료 정보").optional()
    );


    @Test
    void 인기_식물_조회_성공() throws Exception {
        //given
        String token = getTokenForTest((long) (1000*8));

        //when
        ResultActions resultActions =  mockMvc.perform( // api 실행
                RestDocumentationRequestBuilders
                        .get("/api/plants/popular")
                        .param("size","10")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer "+token)
        );
        //then
        resultActions.andExpect(status().isOk());

        //문서화
        resultActions.andDo(document("api/plants/popular/"+1,
                preprocessRequest(prettyPrint(), modifyUris().scheme("https").host("greenroom-server.site").removePort()),
                preprocessResponse(prettyPrint(), getModifiedHeader()),  // (3)
                requestHeaders(headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer : 사용자 access Token")),
                responseFields(resultDescriptorsForPlantsList),
                queryParameters(queryParametersForSize),
                resource(
                        ResourceSnippetParameters.builder()
                                .tag("그린룸") // 문서에서 api들이 태그로 분류됨
                                .summary("많이 키우는 식물 조회 api") // api 이름
                                .description("많이 키우는 식물을 조회함.") // api 설명
                                .build())));
    }


    @Test
    @Transactional
    void 식물_검색_성공() throws Exception {

        //given
        String token = getTokenForTest((long) (1000*8));
        setElsData();;

        //when
        ResultActions resultActions =  mockMvc.perform( // api 실행
                RestDocumentationRequestBuilders
                        .get("/api/plants/search")
                        .param("keyword","드라")
                        .param("size","2")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer "+token)
        );

        cleanupElsData();

        //then
        resultActions.andExpect(status().isOk());

        //문서화
        resultActions.andDo(document("api/plants/search/"+1,
                preprocessRequest(prettyPrint(),modifyUris().scheme("https").host("greenroom-server.site").removePort()),   // (2)
                preprocessResponse(prettyPrint(), getModifiedHeader()),  // (3)
                requestHeaders(headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer : 사용자 access Token")),
                responseFields(resultDescriptorsForPlantsList),
                queryParameters(queryParametersForPlantSearch),
                resource(
                        ResourceSnippetParameters.builder()
                                .tag("그린룸") // 문서에서 api들이 태그로 분류됨
                                .summary("식물 검색 api") // api 이름
                                .description("식물 이름의 특정 키워드로 식물을 검색함.") // api 설명
                                .build())));
    }

    @Test
    void 식물_검색_실패1() throws Exception {

        //given
        String token = getTokenForTest((long) (1000*8));

        String keyword= "몬";

        doThrow(new CustomException(ResponseCodeEnum.FAIL_TO_SEARCH_WITH_ELASTICSEARCH)).when(plantMockitoService).getPlantListWithKeyword(keyword,-1);


        //when
        ResultActions resultActions =  mockMvc.perform( // api 실행
                RestDocumentationRequestBuilders
                        .get("/api/plants/search")
                        .param("keyword",keyword)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer "+token)
        );
        //then
        resultActions.andExpect(status().is(ResponseCodeEnum.FAIL_TO_SEARCH_WITH_ELASTICSEARCH.getStatus().value())).andExpect(jsonPath("code").value(ResponseCodeEnum.FAIL_TO_SEARCH_WITH_ELASTICSEARCH.getCode()));

        //문서화
        resultActions.andDo(document("api/plants/search/"+2,
                preprocessRequest(prettyPrint(),modifyUris().scheme("https").host("greenroom-server.site").removePort()),   // (2)
                preprocessResponse(prettyPrint(), getModifiedHeader()),  // (3)
                requestHeaders(headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer : 사용자 access Token")),
                responseFields(resultDescriptorsForPlantsList),
                queryParameters(queryParametersForPlantSearch),
                resource(
                        ResourceSnippetParameters.builder()
                                .tag("그린룸") // 문서에서 api들이 태그로 분류됨
                                .summary("식물 검색 api") // api 이름
                                .description("식물 이름의 특정 키워드로 식물을 검색함.") // api 설명
                                .build())));
    }

    @Test
    void 식물_물주기_정보_조회_성공 () throws Exception {
        //given
        String token = getTokenForTest((long) (1000*8));

        //when
        ResultActions resultActions = mockMvc.perform( // api 실행
                RestDocumentationRequestBuilders
                        .get("/api/plants/{plantId}/watering-info",1L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer "+token)
        );

        //then
        resultActions.andExpect(status().isOk());

        //문서화
        resultActions.andDo(document("api/plants/watering-info/"+1,
                preprocessRequest(prettyPrint(),modifyUris().scheme("https").host("greenroom-server.site").removePort()),   // (2)
                preprocessResponse(prettyPrint(), getModifiedHeader()),  // (3)
                requestHeaders(headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer : 사용자 access Token")),
                responseFields(resultDescriptorsForPlantWateringInfo),
                pathParameters(pathParametersForPlantId),
                resource(
                        ResourceSnippetParameters.builder()
                                .tag("그린룸") // 문서에서 api들이 태그로 분류됨
                                .summary("식물 물주기 정보 조회 api") // api 이름
                                .description("식물의 물주기 정보를 조호함.") // api 설명
                                .build()))
        );

    }

        @Test
        void 식물_물주기_정보_조회_실패1 () throws Exception {
            //given
            String token = getTokenForTest((long) (1000*8));

            //when
            ResultActions resultActions = mockMvc.perform( // api 실행
                    RestDocumentationRequestBuilders
                            .get("/api/plants/{plantId}/watering-info",1000000L)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer "+token)
            );

            //then
            resultActions.andExpect(status().is(ResponseCodeEnum.PLANT_NOT_FOUND.getStatus().value())).andExpect(jsonPath("code").value(ResponseCodeEnum.PLANT_NOT_FOUND.getCode()));

            //문서화
            resultActions.andDo(document("api/plants/watering-info/"+2,
                    preprocessRequest(prettyPrint(),modifyUris().scheme("https").host("greenroom-server.site").removePort()),   // (2)
                    preprocessResponse(prettyPrint(), getModifiedHeader()),  // (3)
                    requestHeaders(headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer : 사용자 access Token")),
                    responseFields(resultDescriptorsForPlantWateringInfo),
                    pathParameters(pathParametersForPlantId),
                    resource(
                            ResourceSnippetParameters.builder()
                                    .tag("그린룸") // 문서에서 api들이 태그로 분류됨
                                    .summary("식물 물주기 정보 조회 api") // api 이름
                                    .description("식물의 물주기 정보를 조호함.") // api 설명
                                    .build()))
            );
        }

    private ResultActions getResultActionsForPlantInfo(Long plantId) throws Exception {

        String token = getTokenForTest((long) (10*1000));
        return mockMvc.perform( // api 실행
                RestDocumentationRequestBuilders
                        .get("/api/plants/{plantId}",plantId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer "+token));
    }

    private RestDocumentationResultHandler getDocumentForPlantInfo(Integer identifier){
        return document("api/plants/info/"+identifier,
                preprocessRequest(prettyPrint(),modifyUris().scheme("https").host("greenroom-server.site").removePort()),
                preprocessResponse(prettyPrint(), getModifiedHeader()),
                pathParameters(pathParametersForPlantId),
                responseFields(resultDescriptorsForPlantDetails), // responseBody 설명
                requestHeaders(headerWithName("Authorization").description("Bearer : 사용자 access Token")),
                resource(ResourceSnippetParameters.builder()
                        .tag("그린룸") // 문서에서 api들이 태그로 분류됨
                        .summary("식물 상세 정보 조회 api") // api 이름
                        .description("식물 사전에서 식물 상세 정보를 조회함.") // api 설명
                        .build()));
    }

    @Test
    @Transactional
    public void 식물정보_조회_성공() throws Exception {
        //given

        //when
        ResultActions resultActions = getResultActionsForPlantInfo(10L);

        //then
        resultActions.andExpect(status().isOk());

        //문서화
        resultActions.andDo(getDocumentForPlantInfo(1));
    }

    @Test
    @Transactional
    public void 식물정보_조회_실패() throws Exception {
        //given

        //when
        ResultActions resultActions = getResultActionsForPlantInfo(10000L);

        //then
        resultActions.andExpect(status().is(ResponseCodeEnum.PLANT_NOT_FOUND.getStatus().value())).andExpect(jsonPath("code").value(ResponseCodeEnum.PLANT_NOT_FOUND.getCode()));

        //문서화
        resultActions.andDo(getDocumentForPlantInfo(2));
    }
}
