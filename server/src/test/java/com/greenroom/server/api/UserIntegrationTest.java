package com.greenroom.server.api;

import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.greenroom.server.api.config.TestExecutionListener;
import com.greenroom.server.api.domain.user.dto.UserNameUpdateDto;
import com.greenroom.server.api.domain.user.repository.GradeRepository;
import com.greenroom.server.api.domain.user.dto.UserExitRequestDto;
import com.greenroom.server.api.domain.user.entity.User;
import com.greenroom.server.api.domain.user.repository.UserRepository;
import com.greenroom.server.api.domain.user.service.UserService;
import com.greenroom.server.api.enums.ResponseCodeEnum;
import com.greenroom.server.api.exception.CustomException;
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
import org.springframework.mock.web.MockMultipartFile;
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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
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
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.partWithName;
import static org.springframework.restdocs.request.RequestDocumentation.requestParts;
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

public class UserIntegrationTest {

    private static final String EMAIL ="testEmail@gmail.com";
    private static final String PW = "!123456";

    private static final String NAME= "user1";

    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private GradeRepository gradeRepository;

    @MockitoSpyBean
    private UserService userService;

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
            , fieldWithPath("data").type(JsonFieldType.OBJECT).optional().description("data")
    );

    private final List<FieldDescriptor> resultDescriptorsForExitReason = List.of(
            fieldWithPath("status").type(JsonFieldType.STRING).description("응답 상태")
            , fieldWithPath("code").type(JsonFieldType.STRING).description("상태 코드")
            , fieldWithPath("data").type(JsonFieldType.ARRAY).optional().description("data")
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

    public  User signupForTest(){
        User user = User.createUser(new SignupRequestDto(EMAIL,PW,NAME), gradeRepository.findById(1L).orElse(null));
        userRepository.save(user);
        return user;

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
        User user = User.createUser(new SignupRequestDto(EMAIL,PW,NAME), gradeRepository.findById(1L).orElse(null));
        userRepository.save(user);
        refreshTokenRepository.save(RefreshToken.builder().user(user).refreshToken(token).build());

        //when
        ResultActions resultActions = getResultActionsForLogout(token);

        //then
        resultActions
                .andExpect(status().isNoContent())
                .andDo(document("api/users/logout/" + 1
                        ,
                        preprocessRequest(prettyPrint(),modifyUris().scheme("https").host("greenroom-server.site").removePort()),   // (2)
                        preprocessResponse(prettyPrint(), getModifiedHeader()),  // (3)
                        requestHeaders(headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer : 사용자 access Token")),
                        resource(
                                ResourceSnippetParameters.builder()
                                        .tag("User-회원 관련") // 문서에서 api들이 태그로 분류됨
                                        .summary("로그아웃 요청 api") // api 이름
                                        .description("현재 로그인된 사용자를 로그아웃 시키고, 저장된 refresh token을 무효화합니다.") // api 설명
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
                        preprocessRequest(prettyPrint(),modifyUris().scheme("https").host("greenroom-server.site").removePort()),   // (2)
                        preprocessResponse(prettyPrint(), getModifiedHeader()),  // (3)
                        responseFields(resultDescriptors), // responseBody 설명
                        requestHeaders(headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer : 사용자 access Token")),
                        resource(
                                ResourceSnippetParameters.builder()
                                        .tag("User-회원 관련") // 문서에서 api들이 태그로 분류됨
                                        .summary("로그아웃 요청 api") // api 이름
                                        .description("현재 로그인된 사용자를 로그아웃 시키고, 저장된 refresh token을 무효화합니다.") // api 설명
                                        .build())));
    }

    @Test
    public void 탈퇴_사유_조회_성공() throws Exception {

        //given
        String token = getTokenForTest((long) (15*60*1000));

        //when
        ResultActions resultActions = mockMvc.perform( // api 실행
                RestDocumentationRequestBuilders
                        .get("/api/users/exitReasons")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer "+token)
        );

        //then
        resultActions.andExpect(status().isOk());

        //문서화
        resultActions.andDo(
                        document("api/users/exitReason/1" ,
                            preprocessRequest(prettyPrint(),modifyUris().scheme("https").host("greenroom-server.site").removePort()),   // (2)
                            preprocessResponse(prettyPrint(), getModifiedHeader()),  // (3)
                            responseFields(resultDescriptorsForExitReason), // responseBody 설명
                            requestHeaders(headerWithName("Authorization").description("Bearer : 사용자 access Token")),
                            resource(
                                ResourceSnippetParameters.builder()
                                .tag("User-회원 관련") // 문서에서 api들이 태그로 분류됨
                                .summary("탈퇴 사유 조회 api") // api 이름
                                .description("미리 정의된 회원 탈퇴 사유를 조회합니다.") // api 설명
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

        //given
        User user = User.createUser(new SignupRequestDto(EMAIL,PW,NAME), gradeRepository.findById(1L).orElse(null));
        userRepository.save(user);

        String token = getTokenForTest((long) (15*60*1000));

        //when
        ResultActions resultActions = getResultActionsForDeactivation(token);

        //then
        resultActions.andExpect(status().is(ResponseCodeEnum.NO_CONTENT.getStatus().value()));

        //문서화
        resultActions.andDo(document("api/users/delete/" + 1,
                        preprocessRequest(prettyPrint(),modifyUris().scheme("https").host("greenroom-server.site").removePort()),   // (2)
                        preprocessResponse(prettyPrint(), getModifiedHeader()),  // (3)
                        requestHeaders(headerWithName("Authorization").description("Bearer : 사용자 access Token")),
                        requestFields(requestDescriptorsForDeactivation),
                        resource(
                                ResourceSnippetParameters.builder()
                                        .tag("User-회원 관련") // 문서에서 api들이 태그로 분류됨
                                        .summary("탈퇴 api") // api 이름
                                        .description("회원 탈퇴 사유를 처리하고, 회원을 삭제 대기 상태로 전환 합니다. 90일 뒤 회원과 관련한 모든 정보르 삭제합니다.") // api 설명
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
        resultActions.andExpect(status().is(ResponseCodeEnum.USER_NOT_FOUND.getStatus().value())).andExpect(MockMvcResultMatchers.jsonPath("code").value(ResponseCodeEnum.USER_NOT_FOUND.getCode()));

        //문서화
        resultActions.andDo(document("api/users/delete/" + 2,
                        preprocessRequest(prettyPrint(),modifyUris().scheme("https").host("greenroom-server.site").removePort()),   // (2)
                        preprocessResponse(prettyPrint(), getModifiedHeader()),  // (3)
                        responseFields(resultDescriptors), // responseBody 설명
                        requestHeaders(headerWithName("Authorization").description("Bearer : 사용자 access Token")),
                        requestFields(requestDescriptorsForDeactivation),
                        resource(
                                ResourceSnippetParameters.builder()
                                        .tag("User-회원 관련") // 문서에서 api들이 태그로 분류됨
                                        .summary("탈퇴 api") // api 이름
                                        .description("회원 탈퇴 사유를 처리하고, 회원을 삭제 대기 상태로 전환 합니다. 90일 뒤 회원과 관련한 모든 정보르 삭제합니다.") // api 설명
                                        .build())));
    }
    private final List<FieldDescriptor> resultDescriptorsForUserInfo = List.of(
            fieldWithPath("status").type(JsonFieldType.STRING).description("응답 상태"),
            fieldWithPath("code").type(JsonFieldType.STRING).description("상태 코드"),
            fieldWithPath("data").type(JsonFieldType.OBJECT).optional().description("data"),
            fieldWithPath("data.userName").type(JsonFieldType.STRING).description("user 이름"),
            fieldWithPath("data.email").type(JsonFieldType.STRING).description("user email"),
            fieldWithPath("data.userDurationWithGreenroom").type(JsonFieldType.NUMBER).description("user 가입 일수"),
            fieldWithPath("data.level").type(JsonFieldType.NUMBER).description("user level"),
            fieldWithPath("data.levelName").type(JsonFieldType.STRING).description("user level 이름"),
            fieldWithPath("data.profileImgUrl").type(JsonFieldType.STRING).description("user profile image url").optional().attributes(new Attributes.Attribute("constraint","등록된 이미지가 없으면 null")),
            fieldWithPath("data.seedsToNextLevel").type(JsonFieldType.NUMBER).description("다음 레벨까지 남은 씨앗 개수")
    );
    private ResultActions getResultActionsForUserInfo() throws Exception {

        String token = getTokenForTest((long) (15*60*1000));

        return mockMvc.perform( // api 실행
                RestDocumentationRequestBuilders
                        .get("/api/users/info")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer "+token));
    }

    private RestDocumentationResultHandler getDocumentForUserInfo(Integer identifier){
        return document("api/users/info/" + identifier,
                preprocessRequest(prettyPrint(),modifyUris().scheme("https").host("greenroom-server.site").removePort()),   // (2)
                preprocessResponse(prettyPrint(), getModifiedHeader()),  // (3)
                responseFields(resultDescriptorsForUserInfo), // responseBody 설명
                requestHeaders(headerWithName("Authorization").description("Bearer : 사용자 access Token")),
                resource(
                        ResourceSnippetParameters.builder()
                                .tag("User-회원 관련") // 문서에서 api들이 태그로 분류됨
                                .summary("회원 정보 조회 api") // api 이름
                                .description("user의 기본 정보를 조회함.") // api 설명
                                .build()));
    }

    @Test
    @Transactional
    public void user_info_get_성공() throws Exception {
        //given
        User user = signupForTest();
        user.updateCreateDate(LocalDateTime.now().minusDays(3));

        //when
        ResultActions resultActions = getResultActionsForUserInfo();

        //then
        resultActions.andExpect(status().isOk());

        //문서화
        resultActions.andDo(getDocumentForUserInfo(1));
    }

    @Test
    @Transactional
    public void user_info_get_실패1() throws Exception {

        //when
        ResultActions resultActions = getResultActionsForUserInfo();

        resultActions.andExpect(status().is(ResponseCodeEnum.USER_NOT_FOUND.getStatus().value())).andExpect(jsonPath("code").value(ResponseCodeEnum.USER_NOT_FOUND.getCode()));

        //문서화
        resultActions.andDo(getDocumentForUserInfo(2));
    }


    private ResultActions getResultActionsUpdateUserName(UserNameUpdateDto userNameUpdateDto) throws Exception {

        String token = getTokenForTest((long) (15*60*1000));

        return mockMvc.perform( // api 실행
                RestDocumentationRequestBuilders
                        .patch("/api/users/name")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer "+token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(userNameUpdateDto)));

    }


    private final List<FieldDescriptor> requestDescriptorForUpdateUserName = List.of(
            fieldWithPath("user_name").type(JsonFieldType.STRING).description("변경하고자 하는 user name")
    );
    private final List<FieldDescriptor> resultDescriptorsForUpdateUserName = List.of(
            fieldWithPath("status").type(JsonFieldType.STRING).description("응답 상태"),
            fieldWithPath("code").type(JsonFieldType.STRING).description("상태 코드"),
            fieldWithPath("data").type(JsonFieldType.OBJECT).optional().description("data"),
            fieldWithPath("data.user_name").type(JsonFieldType.STRING).description("변경된 user name").optional()
    );

    private RestDocumentationResultHandler getDocumentForUpdateUserNAme (Integer identifier){
        return document("api/users/name/" + identifier,
                preprocessRequest(prettyPrint(),modifyUris().scheme("https").host("greenroom-server.site").removePort()),   // (2)
                preprocessResponse(prettyPrint(), getModifiedHeader()),  // (3)
                requestFields(requestDescriptorForUpdateUserName),
                responseFields(resultDescriptorsForUpdateUserName), // responseBody 설명
                requestHeaders(headerWithName("Authorization").description("Bearer : 사용자 access Token")),
                resource(
                        ResourceSnippetParameters.builder()
                                .tag("User-회원 관련") // 문서에서 api들이 태그로 분류됨
                                .summary("user name 변경 api") // api 이름
                                .description("user의 nickname을 변경함.") // api 설명
                                .build()));
    }


    @Test
    @Transactional
    public void user_name_변경_성공() throws Exception {
        //given
        signupForTest();
        UserNameUpdateDto userNameUpdateDto = new UserNameUpdateDto("user11111");

        //when
        ResultActions resultActions = getResultActionsUpdateUserName(userNameUpdateDto);

        //then
        resultActions.andExpect(status().isOk());

        //문서화
        resultActions.andDo(getDocumentForUpdateUserNAme(1));
    }

    @Test
    @Transactional
    public void user_name_변경_실패1() throws Exception {

        //given
        UserNameUpdateDto userNameUpdateDto = new UserNameUpdateDto("user11111");

        //when
        ResultActions resultActions = getResultActionsUpdateUserName(userNameUpdateDto);

        //then
        resultActions.andExpect(status().is(ResponseCodeEnum.USER_NOT_FOUND.getStatus().value())).andExpect(jsonPath("code").value(ResponseCodeEnum.USER_NOT_FOUND.getCode()));

        //문서화
        resultActions.andDo(getDocumentForUpdateUserNAme(2));
    }

    private ResultActions getResultActionsForDeleteUserProfileImage() throws Exception {

        String token = getTokenForTest((long) (15*60*1000));

        return mockMvc.perform( // api 실행
                RestDocumentationRequestBuilders
                        .delete("/api/users/profile-image")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer "+token));

    }

    @Test
    @Transactional
    public void user_profile_image_삭제_성공() throws Exception {

        //given
        signupForTest();

        //when
        ResultActions resultActions = getResultActionsForDeleteUserProfileImage();

        //then
        resultActions.andExpect(status().isNoContent());

        //문서화
        resultActions.andDo(document("api/users/profile-image/delete/" + 1,
                preprocessRequest(prettyPrint(),modifyUris().scheme("https").host("greenroom-server.site").removePort()),   // (2)
                preprocessResponse(prettyPrint(), getModifiedHeader()),  // (3)
                requestHeaders(headerWithName("Authorization").description("Bearer : 사용자 access Token")),
                resource(
                        ResourceSnippetParameters.builder()
                                .tag("User-회원 관련") // 문서에서 api들이 태그로 분류됨
                                .summary("user profile image 삭제 api") // api 이름
                                .description("user의 profile image를 삭제함.") // api 설명
                                .build())));
    }

    @Test
    @Transactional
    public void user_profile_image_삭제_실패1() throws Exception {

        //when
        ResultActions resultActions = getResultActionsForDeleteUserProfileImage();

        //then
        resultActions.andExpect(status().is(ResponseCodeEnum.USER_NOT_FOUND.getStatus().value())).andExpect(jsonPath("code").value(ResponseCodeEnum.USER_NOT_FOUND.getCode()));

        //문서화
        resultActions.andDo(document("api/users/profile-image/delete/" + 2,
                preprocessRequest(prettyPrint(),modifyUris().scheme("https").host("greenroom-server.site").removePort()),   // (2)
                preprocessResponse(prettyPrint(), getModifiedHeader()),  // (3)
                responseFields(resultDescriptors), // responseBody 설명
                requestHeaders(headerWithName("Authorization").description("Bearer : 사용자 access Token")),
                resource(
                        ResourceSnippetParameters.builder()
                                .tag("User-회원 관련") // 문서에서 api들이 태그로 분류됨
                                .summary("user profile image 삭제 api") // api 이름
                                .description("user의 profile image를 삭제함.") // api 설명
                                .build())));

    }

    private MockMultipartFile getTestMultiPartFile (){

        String filePath = "src/test/resources/test.jpg"; //test 이미지 파일경로

        try(FileInputStream fileInputStream = new FileInputStream(filePath)){
            return new MockMultipartFile("profile_image_file", "test.jpg", "image/jpg", fileInputStream);
        }
        catch (IOException e){
            throw new RuntimeException();
        }
    }

    private MockMultipartFile getInvalidTestMultiPartFile (){

        String filePath = "src/test/resources/test.jpg"; //test 이미지 파일경로

        try(FileInputStream fileInputStream = new FileInputStream(filePath)){
            return new MockMultipartFile("profile_image_file", "test.jpg", "text/plain", fileInputStream);
        }
        catch (IOException e){
            throw new RuntimeException();
        }
    }


    private ResultActions getResultActionsForPostUserProfileImage(MockMultipartFile mockMultipartFile) throws Exception {
        String token = getTokenForTest((long) (15*60*1000));

        return mockMvc.perform( // api 실행
                RestDocumentationRequestBuilders
                        .multipart("/api/users/profile-image")
                        .file(mockMultipartFile)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer "+token));
    }

    private final List<FieldDescriptor> resultDescriptorsForPostUserImage = List.of(
            fieldWithPath("status").type(JsonFieldType.STRING).description("응답 상태"),
            fieldWithPath("code").type(JsonFieldType.STRING).description("상태 코드"),
            fieldWithPath("data").type(JsonFieldType.OBJECT).optional().description("data"),
            fieldWithPath("data.image_url").type(JsonFieldType.STRING).description("user 이미지 url").optional()
    );

    private RestDocumentationResultHandler getDocumentForPostUserProfileImage(Integer identifier){
        return document("api/users/profile-image/post/" + identifier,
                preprocessRequest(prettyPrint(),modifyUris().scheme("https").host("greenroom-server.site").removePort()),   // (2)
                preprocessResponse(prettyPrint(), getModifiedHeader()),  // (3)
                requestParts(partWithName("profile_image_file").description("user 프로필 이미지 파일").attributes(new Attributes.Attribute("content-type","image/*"))),
                responseFields(resultDescriptorsForPostUserImage), // responseBody 설명
                requestHeaders(headerWithName("Authorization").description("Bearer : 사용자 access Token")),
                resource(
                        ResourceSnippetParameters.builder()
                                .tag("User-회원 관련") // 문서에서 api들이 태그로 분류됨
                                .summary("user profile image 등록 api") // api 이름
                                .description("user의 profile image를 등록/갱신함.") // api 설명
                                .build()));
    }


    @Test
    @Transactional
    public void user_profile_image_등록_성공() throws Exception {
        //given
        signupForTest();
        MockMultipartFile mockMultipartFile = getTestMultiPartFile();

        //when
        ResultActions resultActions =  getResultActionsForPostUserProfileImage(mockMultipartFile);

        //then
        resultActions.andExpect(status().isCreated());

        //문서화
        resultActions.andDo(getDocumentForPostUserProfileImage(1));
    }

    @Test
    @Transactional
    public void user_profile_image_등록_실패1() throws Exception {
        //given
        MockMultipartFile mockMultipartFile = getTestMultiPartFile();

        //when
        ResultActions resultActions =  getResultActionsForPostUserProfileImage(mockMultipartFile);

        //then
        resultActions.andExpect(status().is(ResponseCodeEnum.USER_NOT_FOUND.getStatus().value())).andExpect(jsonPath("code").value(ResponseCodeEnum.USER_NOT_FOUND.getCode()));

        //문서화
        resultActions.andDo(getDocumentForPostUserProfileImage(2));
    }

    @Test
    @Transactional
    public void user_profile_image_등록_실패2() throws Exception {

        //given
        signupForTest();
        MockMultipartFile mockMultipartFile = getTestMultiPartFile();

        //when
        doThrow(new CustomException(ResponseCodeEnum.FAIL_TO_UPLOAD_IMAGE)).when(userService).uploadUserProfileImage(EMAIL,mockMultipartFile);
        ResultActions resultActions =  getResultActionsForPostUserProfileImage(mockMultipartFile);

        //then
        resultActions.andExpect(status().is(ResponseCodeEnum.FAIL_TO_UPLOAD_IMAGE.getStatus().value())).andExpect(jsonPath("code").value(ResponseCodeEnum.FAIL_TO_UPLOAD_IMAGE.getCode()));

        //문서화
        resultActions.andDo(getDocumentForPostUserProfileImage(3));

    }

    @Test
    @Transactional
    public void user_profile_image_등록_실패3() throws Exception {
        //given
        signupForTest();

        //when
        ResultActions resultActions =  getResultActionsForPostUserProfileImage(getInvalidTestMultiPartFile());

        //then
        resultActions.andExpect(status().is(ResponseCodeEnum.INVALID_IMAGE_FORMAT.getStatus().value())).andExpect(jsonPath("code").value(ResponseCodeEnum.INVALID_IMAGE_FORMAT.getCode()));

        //문서화
        resultActions.andDo(getDocumentForPostUserProfileImage(4));

    }
}
