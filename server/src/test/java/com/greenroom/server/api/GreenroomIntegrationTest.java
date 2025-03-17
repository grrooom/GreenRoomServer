package com.greenroom.server.api;

import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.greenroom.server.api.config.TestExecutionListener;
import com.greenroom.server.api.domain.greenroom.dto.in.CompleteTodoRequestDto;
import com.greenroom.server.api.domain.greenroom.dto.in.GreenroomDecorationRequestDto;
import com.greenroom.server.api.domain.greenroom.dto.in.GreenroomRegistrationRequestDto;
import com.greenroom.server.api.domain.greenroom.entity.Adornment;
import com.greenroom.server.api.domain.greenroom.entity.GreenRoom;
import com.greenroom.server.api.domain.greenroom.entity.Plant;
import com.greenroom.server.api.domain.greenroom.entity.Todo;
import com.greenroom.server.api.domain.greenroom.repository.*;
import com.greenroom.server.api.domain.greenroom.service.GreenroomService;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler;
import org.springframework.restdocs.operation.preprocess.HeadersModifyingOperationPreprocessor;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.restdocs.request.ParameterDescriptor;
import org.springframework.restdocs.request.RequestPartDescriptor;
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

import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDate;
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
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.partWithName;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@AutoConfigureMockMvc
@AutoConfigureRestDocs
@SpringBootTest
@ActiveProfiles("test")
@ExtendWith({RestDocumentationExtension.class})
@TestExecutionListeners(value = TestExecutionListener.class, mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
public class GreenroomIntegrationTest {

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
    private PlantRepository plantRepository;

    @Autowired
    private GreenRoomRepository greenRoomRepository;

    @Autowired
    private TodoRepository todoRepository;

    @Autowired
    private AdornmentRepository adornmentRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private ActivityRepository activityRepository;

    @MockitoSpyBean
    private GreenroomService mockitoGreenroomService;

    private final ObjectMapper mapper = new ObjectMapper();


    @BeforeEach
    void setup(WebApplicationContext context , RestDocumentationContextProvider restDocumentation) {
        mapper.registerModule(new JavaTimeModule());
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

    public GreenRoom createGreenRoom(User user){

        Plant plant = plantRepository.findById(20L).get();
        //plantRepository.save(plant);

        GreenRoom greenRoom = GreenRoom.of("test 그린룸",null,user,plant);

        greenRoomRepository.save(greenRoom);

        Todo todo = Todo.builder().greenRoom(greenRoom).activity(activityRepository.findAll().get(0)).nextTodoDate(LocalDate.now().plusDays(1)).term(10).build();
        todoRepository.save(todo);

        adornmentRepository.save(Adornment.createAdornment(itemRepository.findAll().get(0),greenRoom));


        return greenRoom;
    }

    private MockMultipartFile getTestMultiPartFile (){

        String filePath = "src/test/resources/test.jpg"; //test 이미지 파일경로

        try(FileInputStream fileInputStream = new FileInputStream(filePath)){
            return new MockMultipartFile("imageFile", "test.jpg", "image/jpg", fileInputStream);
        }
        catch (IOException e){
            throw new RuntimeException();
        }
    }
    private MockMultipartFile getInvalidTestMultiPartFile (){

        String filePath = "src/test/resources/test.jpg"; //test 이미지 파일경로

        try(FileInputStream fileInputStream = new FileInputStream(filePath)){
            return new MockMultipartFile("imageFile", "test.jpg", "text/plain", fileInputStream);
        }
        catch (IOException e){
            throw new RuntimeException();
        }
    }

    List<FieldDescriptor> resultDescriptorsForGetGreenroom = List.of(
            fieldWithPath("status").type(JsonFieldType.STRING).description("응답 상태"),
            fieldWithPath("code").type(JsonFieldType.STRING).description("상태 코드"),
            fieldWithPath("data").type(JsonFieldType.OBJECT).optional().description("data").attributes(new Attributes.Attribute("constraint","등록된 식물이 없으면 null")),
            fieldWithPath("data.basicInfo").type(JsonFieldType.OBJECT).description("그린룸 기본 정보"),
            fieldWithPath("data.basicInfo.greenroomId").type(JsonFieldType.NUMBER).description("그린룸 id"),
            fieldWithPath("data.basicInfo.plantNickname").type(JsonFieldType.STRING).description("그린룸 별명"),
            fieldWithPath("data.basicInfo.plantName").type(JsonFieldType.STRING).description("식물 이름").optional().attributes(new Attributes.Attribute("constraint","등록된 식물 종류가 없으면 null")),
            fieldWithPath("data.basicInfo.imageUrl").type(JsonFieldType.STRING).description("사용자가 등록한 식물 이미지.").optional().attributes(new Attributes.Attribute("constraint","등록된 사진이 없으면 null")),
            fieldWithPath("data.basicInfo.memo").type(JsonFieldType.STRING).description("사용자가 등록한 식물 메모").optional().attributes(new Attributes.Attribute("constraint","등록된 메모가 없으면 null")),
            fieldWithPath("data.todo").type(JsonFieldType.OBJECT).description("그린룸 할 일 정보"),
            fieldWithPath("data.todo.todoList").type(JsonFieldType.ARRAY).description("할 일 목록.").attributes(new Attributes.Attribute("constraint","목록이 없으면 빈 배열 반환")),
            fieldWithPath("data.todo.todoList[].activityId").type(JsonFieldType.NUMBER).description("할 일 id").optional(),
            fieldWithPath("data.todo.todoList[].activityName").type(JsonFieldType.STRING).description("할 일 이름").optional(),
            fieldWithPath("data.todo.todoList[].description").type(JsonFieldType.STRING).description("한국어 설명").optional(),
            fieldWithPath("data.todo.numberOfTodo").type(JsonFieldType.NUMBER).description("할 일 총 개수"),
            fieldWithPath("data.customItems").type(JsonFieldType.OBJECT).description("사용자가 등록한 그린룸 custom item"),
            fieldWithPath("data.customItems.shape").type(JsonFieldType.OBJECT).description("식물 형태"),
            fieldWithPath("data.customItems.shape.itemId").type(JsonFieldType.NUMBER).description("식물 형태 item id").optional(),
            fieldWithPath("data.customItems.shape.itemName").type(JsonFieldType.STRING).description("식물 형태 item 이름").optional(),
            fieldWithPath("data.customItems.hairAccessory").type(JsonFieldType.OBJECT).description("헤어핀 악세서리").attributes(new Attributes.Attribute("constraint","등록된 item이 없으면 null")).optional(),
            fieldWithPath("data.customItems.hairAccessory.itemId").type(JsonFieldType.NUMBER).description("헤어핀 악세서리 item id").optional(),
            fieldWithPath("data.customItems.hairAccessory.itemName").type(JsonFieldType.STRING).description("헤어핀 악세서리 item 이름").optional(),
            fieldWithPath("data.customItems.eyewear").type(JsonFieldType.OBJECT).description("안경 악세서리").attributes(new Attributes.Attribute("constraint","등록된 item이 없으면 null")).optional(),
            fieldWithPath("data.customItems.eyewear.itemId").type(JsonFieldType.NUMBER).description("안경 악세서리 item id").optional(),
            fieldWithPath("data.customItems.eyewear.itemName").type(JsonFieldType.STRING).description("안경 악세서리 item 이름").optional(),
            fieldWithPath("data.customItems.shelf").type(JsonFieldType.OBJECT).description("선반 소품").attributes(new Attributes.Attribute("constraint","등록된 item이 없으면 null")).optional(),
            fieldWithPath("data.customItems.shelf.itemId").type(JsonFieldType.NUMBER).description("선반 소품 item id").optional(),
            fieldWithPath("data.customItems.shelf.itemName").type(JsonFieldType.STRING).description("선반 소품 item 이름").optional(),
            fieldWithPath("data.customItems.window").type(JsonFieldType.OBJECT).description("창문 소품").attributes(new Attributes.Attribute("constraint","등록된 item이 없으면 null")).optional(),
            fieldWithPath("data.customItems.window.itemId").type(JsonFieldType.NUMBER).description("창문 소품 item id").optional(),
            fieldWithPath("data.customItems.window.itemName").type(JsonFieldType.STRING).description("창문 소품 item 이름").optional()
    );

    private final List<FieldDescriptor> resultDescriptorsForLevelUp = List.of(
            fieldWithPath("status").type(JsonFieldType.STRING).description("응답 상태"),
            fieldWithPath("code").type(JsonFieldType.STRING).description("상태 코드"),
            fieldWithPath("data").type(JsonFieldType.OBJECT).optional().description("data").attributes(new Attributes.Attribute("constraint","실패 응답시 null")),
            fieldWithPath("data.earnedPoints").type(JsonFieldType.NUMBER).description("action 수행으로 획득한 총 point"),
            fieldWithPath("data.levelUpStatus").type(JsonFieldType.OBJECT).description("레벨업 정보"),
            fieldWithPath("data.levelUpStatus.isLevelUp").type(JsonFieldType.BOOLEAN).description("action 수행 후 레벨업 여부"),
            fieldWithPath("data.levelUpStatus.currentLevel").type(JsonFieldType.NUMBER).description("action 수행 후 사용자 레벨"),
            fieldWithPath("data.levelUpDetails").type(JsonFieldType.ARRAY).optional().description("레벨업 했을 경우 상세 정보 - 레벨업의 원인이 되는 모든 action들을 나열함.").attributes(new Attributes.Attribute("constraint","레벨업 상세 정보가 필요하지 않은 경우 null")),
            fieldWithPath("data.levelUpDetails[].source").type(JsonFieldType.STRING).description("레벨업 원인이 되는 개별 action"),
            fieldWithPath("data.levelUpDetails[].points").type(JsonFieldType.NUMBER).description("개별 action 수행으로 획득한 point")
    );

    private final List<FieldDescriptor> resultDescriptorsForNicknameDuplication = List.of(
            fieldWithPath("status").type(JsonFieldType.STRING).description("응답 상태"),
            fieldWithPath("code").type(JsonFieldType.STRING).description("상태 코드"),
            fieldWithPath("data").type(JsonFieldType.BOOLEAN).description("중복 여부. 중복일 때 true")
    );


    private final List<ParameterDescriptor> queryParametersForNickname = List.of(
            parameterWithName("nickname").description("중복을 확인할 nickname")
    );

    private final List<ParameterDescriptor> pathParameterForGreenroomId = List.of(
        parameterWithName("greenroom_id").description("그린룸 id")
    );

    List<RequestPartDescriptor> requestPartDescriptorsForGreenroomRegistration = List.of(
            partWithName("imageFile").description("식물 이미지 파일").attributes(new Attributes.Attribute("content-type","image/*")),
            partWithName("data").description("그린룸 등록 정보").attributes(new Attributes.Attribute("content-type","application/json"))
    );

    List<FieldDescriptor> requestPartFieldDescriptorsForGreenroomRegistration = List.of(
            fieldWithPath("plantId").type(JsonFieldType.NUMBER).description("식물 종류 id").optional(),
            fieldWithPath("nickname").type(JsonFieldType.STRING).description("식물 별명"),
            fieldWithPath("wateringBaseDate").type(JsonFieldType.STRING).description("물주기 기준 날짜").attributes(new Attributes.Attribute("constraint","YYYY-MM-DD")),
            fieldWithPath("wateringInterval").type(JsonFieldType.NUMBER).description("물 주는 주기"),
            fieldWithPath("itemId").type(JsonFieldType.NUMBER).description("식물 형태 id")
    );

    List<FieldDescriptor> requestBodyDescriptorsForCompleteTodo = List.of(
            fieldWithPath("completedTodo").type(JsonFieldType.ARRAY).description("완료 처리할 작업의 id 목록")
    );


    List<FieldDescriptor> requestBodyDescriptorsForAdornment = List.of(
            fieldWithPath("shape").type(JsonFieldType.NUMBER).description("식물 형태 item id"),
            fieldWithPath("eyewear").type(JsonFieldType.NUMBER).description("안경 악세서리 item id").optional().attributes(new Attributes.Attribute("constraint","사용하지 않는 경우 null 또는 request body에서 필드 제외")),
            fieldWithPath("hairAccessory").type(JsonFieldType.NUMBER).description("헤어핀 악세서리 item id").optional().attributes(new Attributes.Attribute("constraint","사용하지 않는 경우 null 또는 request body에서 필드 제외")),
            fieldWithPath("shelf").type(JsonFieldType.NUMBER).description("선반 소품 item id").optional().attributes(new Attributes.Attribute("constraint","사용하지 않는 경우 null 또는 request body에서 필드 제외")),
            fieldWithPath("window").type(JsonFieldType.NUMBER).description("창문 소품 item id").optional().attributes(new Attributes.Attribute("constraint","사용하지 않는 경우 null 또는 request body에서 필드 제외"))
    );

    List<FieldDescriptor> resultDescriptorsForAdornment = List.of(
            fieldWithPath("status").type(JsonFieldType.STRING).description("응답 상태"),
            fieldWithPath("code").type(JsonFieldType.STRING).description("상태 코드"),
            fieldWithPath("data").type(JsonFieldType.OBJECT).optional().description("data").attributes(new Attributes.Attribute("constraint","등록된 식물이 없으면 null")),
            fieldWithPath("data.shape").type(JsonFieldType.OBJECT).description("식물 형태"),
            fieldWithPath("data.shape.itemId").type(JsonFieldType.NUMBER).description("식물 형태 item id").optional(),
            fieldWithPath("data.shape.itemName").type(JsonFieldType.STRING).description("식물 형태 item 이름").optional(),
            fieldWithPath("data.hairAccessory").type(JsonFieldType.OBJECT).description("헤어핀 악세서리").attributes(new Attributes.Attribute("constraint","등록된 item이 없으면 null")).optional(),
            fieldWithPath("data.hairAccessory.itemId").type(JsonFieldType.NUMBER).description("헤어핀 악세서리 item id").optional(),
            fieldWithPath("data.hairAccessory.itemName").type(JsonFieldType.STRING).description("헤어핀 악세서리 item 이름").optional(),
            fieldWithPath("data.eyewear").type(JsonFieldType.OBJECT).description("안경 악세서리").attributes(new Attributes.Attribute("constraint","등록된 item이 없으면 null")).optional(),
            fieldWithPath("data.eyewear.itemId").type(JsonFieldType.NUMBER).description("안경 악세서리 item id").optional(),
            fieldWithPath("data.eyewear.itemName").type(JsonFieldType.STRING).description("안경 악세서리 item 이름").optional(),
            fieldWithPath("data.shelf").type(JsonFieldType.OBJECT).description("선반 소품").attributes(new Attributes.Attribute("constraint","등록된 item이 없으면 null")).optional(),
            fieldWithPath("data.shelf.itemId").type(JsonFieldType.NUMBER).description("선반 소품 item id").optional(),
            fieldWithPath("data.shelf.itemName").type(JsonFieldType.STRING).description("선반 소품 item 이름").optional(),
            fieldWithPath("data.window").type(JsonFieldType.OBJECT).description("창문 소품").attributes(new Attributes.Attribute("constraint","등록된 item이 없으면 null")).optional(),
            fieldWithPath("data.window.itemId").type(JsonFieldType.NUMBER).description("창문 소품 item id").optional(),
            fieldWithPath("data.window.itemName").type(JsonFieldType.STRING).description("창문 소품 item 이름").optional()
    );

    List<FieldDescriptor> resultDescriptorsForGreenroomDetails= List.of(
            fieldWithPath("status").type(JsonFieldType.STRING).description("응답 상태"),
            fieldWithPath("code").type(JsonFieldType.STRING).description("상태 코드"),
            fieldWithPath("data").type(JsonFieldType.OBJECT).optional().description("data").optional().attributes(new Attributes.Attribute("constraint","등록된 식물이 없으면 null")),
            fieldWithPath("data.basicInfo").type(JsonFieldType.OBJECT).optional().description("그린룸 기본 정보"),
            fieldWithPath("data.basicInfo.greenroomId").type(JsonFieldType.NUMBER).description("그린룸 고유 id"),
            fieldWithPath("data.basicInfo.nickName").type(JsonFieldType.STRING).description("그린룸 이름"),
            fieldWithPath("data.basicInfo.plantName").type(JsonFieldType.STRING).description("식물 이름"),
            fieldWithPath("data.basicInfo.duration").type(JsonFieldType.NUMBER).description("함께한 기간"),
            fieldWithPath("data.basicInfo.memo").type(JsonFieldType.STRING).description("메모").optional().attributes(new Attributes.Attribute("constraint","등록된 memo가 없으면 null")),
            fieldWithPath("data.basicInfo.imageUrl").type(JsonFieldType.STRING).description("그린룸 이미지 url").optional().attributes(new Attributes.Attribute("constraint","등록된 이미지가 없으면 null")),
            fieldWithPath("data.decoration").type(JsonFieldType.OBJECT).description("그린룸 꾸미기 item"),
            fieldWithPath("data.decoration.shape").type(JsonFieldType.OBJECT).description("형태"),
            fieldWithPath("data.decoration.shape.itemId").type(JsonFieldType.NUMBER).description("item 고유 id"),
            fieldWithPath("data.decoration.shape.itemName").type(JsonFieldType.STRING).description("item 이름"),
            fieldWithPath("data.decoration.eyewear").type(JsonFieldType.OBJECT).optional().description("안경 item").attributes(new Attributes.Attribute("constraint","등록된 item이 없으면 null")),
            fieldWithPath("data.decoration.eyewear.itemId").type(JsonFieldType.NUMBER).optional().description("item 고유 id"),
            fieldWithPath("data.decoration.eyewear.itemName").type(JsonFieldType.STRING).optional().description("item 이름"),
            fieldWithPath("data.decoration.hairAccessory").type(JsonFieldType.OBJECT).optional().description("헤어핀 item").attributes(new Attributes.Attribute("constraint","등록된 item이 없으면 null")),
            fieldWithPath("data.decoration.hairAccessory.itemId").type(JsonFieldType.NUMBER).optional().description("item 고유 id"),
            fieldWithPath("data.decoration.hairAccessory.itemName").type(JsonFieldType.STRING).optional().description("item 이름"),
            fieldWithPath("data.managementInfo").type(JsonFieldType.ARRAY).description("사용자가 등록한 그린룸 관리 정보 목록").attributes(new Attributes.Attribute("constraint","등록된 관리 정보가 없으면 빈 배열 반환")),
            fieldWithPath("data.managementInfo[].activityId").type(JsonFieldType.NUMBER).description("할 일 고유 id"),
            fieldWithPath("data.managementInfo[].activityName").type(JsonFieldType.STRING).description("할 일 이름"),
            fieldWithPath("data.managementInfo[].term").type(JsonFieldType.NUMBER).description("할 일 주기"),
            fieldWithPath("data.managementInfo[].remainingDays").type(JsonFieldType.NUMBER).description("다음 수행일까지 남은 기간"),
            fieldWithPath("data.plantInfo").type(JsonFieldType.OBJECT).description("식물 기본 정보"),
            fieldWithPath("data.plantInfo.plantId").type(JsonFieldType.NUMBER).description("식물 id"),
            fieldWithPath("data.plantInfo.name").type(JsonFieldType.STRING).description("식물 이름"),
            fieldWithPath("data.plantInfo.scientificName").type(JsonFieldType.STRING).description("식물 학명"),
            fieldWithPath("data.plantInfo.description").type(JsonFieldType.STRING).description("식물에 대한 설명"),
            fieldWithPath("data.plantManagementInfo").type(JsonFieldType.OBJECT).description("식물 키우는 법"),
            fieldWithPath("data.plantManagementInfo.managementLevel").type(JsonFieldType.STRING).description("관리 레벨 정보"),
            fieldWithPath("data.plantManagementInfo.temperature").type(JsonFieldType.STRING).description("온도 정보"),
            fieldWithPath("data.plantManagementInfo.sunlight").type(JsonFieldType.STRING).description("햇빛 정보"),
            fieldWithPath("data.plantManagementInfo.watering").type(JsonFieldType.STRING).description("물주기 정보"),
            fieldWithPath("data.plantManagementInfo.humidity").type(JsonFieldType.STRING).description("습도 정보"),
            fieldWithPath("data.plantManagementInfo.fertilizer").type(JsonFieldType.STRING).description("비료 정보")
            );
    @Transactional
    @Test
    public void 그린룸_정보_조회_성공1() throws Exception {

        //given
        User user = signupForTest();
        createGreenRoom(user);
        String token = getTokenForTest((long) (10*1000));

        //when
        ResultActions resultActions =  mockMvc.perform( // api 실행
                RestDocumentationRequestBuilders
                        .get("/api/greenroom/info")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer "+token)
        );

        //then
        resultActions.andExpect(status().isOk());

        //문서화
        resultActions.andDo(document("api/greenroom/info/"+1
                ,
                preprocessRequest(prettyPrint(),modifyUris().scheme("https").host("greenroom-server.site").removePort()),   // (2)
                preprocessResponse(prettyPrint(), getModifiedHeader()),  // (3)
                requestHeaders(headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer : 사용자 access Token")),
                responseFields(resultDescriptorsForGetGreenroom),
                resource(
                        ResourceSnippetParameters.builder()
                                .tag("그린룸") // 문서에서 api들이 태그로 분류됨
                                .summary("user 그린룸 정보 조회") // api 이름
                                .description("사용자의 그린룸 정보를 조회함.") // api 설명
                                .build())));
    }

    @Transactional
    @Test
    public void 그린룸_정보_조회_성공2() throws Exception {

        //given
        signupForTest();
        String token = getTokenForTest((long) (10*1000));

        //when
        ResultActions resultActions =  mockMvc.perform( // api 실행
                RestDocumentationRequestBuilders
                        .get("/api/greenroom/info")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer "+token)
        );

        //then
        resultActions.andExpect(status().isOk());

        //문서화
        resultActions.andDo(document("api/greenroom/info/"+2
                ,
                preprocessRequest(prettyPrint(),modifyUris().scheme("https").host("greenroom-server.site").removePort()),   // (2)
                preprocessResponse(prettyPrint(), getModifiedHeader()),  // (3)
                requestHeaders(headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer : 사용자 access Token")),
                responseFields(resultDescriptorsForGetGreenroom),
                resource(
                        ResourceSnippetParameters.builder()
                                .tag("그린룸") // 문서에서 api들이 태그로 분류됨
                                .summary("user 그린룸 정보 조회") // api 이름
                                .description("사용자의 그린룸 정보를 조회함.") // api 설명
                                .build())));
    }

    @Test
    @Transactional
    void 닉네임_중복확인_성공() throws Exception {

        //given
        signupForTest();
        String token = getTokenForTest((long) (10*1000));

        //when
        ResultActions resultActions =  mockMvc.perform( // api 실행
                RestDocumentationRequestBuilders
                        .get("/api/greenroom/nickname/duplication")
                        .param("nickname","초롱이")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer "+token)
        );

        //then
        resultActions.andExpect(status().isOk());

        //문서화
        resultActions.andDo(document("api/greenroom/nickname/duplication/"+1,
                preprocessRequest(prettyPrint(),modifyUris().scheme("https").host("greenroom-server.site").removePort()),   // (2)
                preprocessResponse(prettyPrint(), getModifiedHeader()),  // (3)
                requestHeaders(headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer : 사용자 access Token")),
                responseFields(resultDescriptorsForNicknameDuplication),
                queryParameters(queryParametersForNickname),
                resource(
                        ResourceSnippetParameters.builder()
                                .tag("그린룸") // 문서에서 api들이 태그로 분류됨
                                .summary("닉네임 중복 확인 api") // api 이름
                                .description("그린룸 닉네임 중복 확인. user 끼리는 중복 허용. 한명의 user가 같은 별명을 여러번 사용하는 것이 제한됨.") // api 설명
                                .build())));
    }


    private ResultActions getResultActionsForGreenroomRegistration(MockMultipartFile image,MockMultipartFile data) throws Exception {

        String token = getTokenForTest((long) (10*1000));

        return mockMvc.perform( // api 실행
                RestDocumentationRequestBuilders
                        .multipart("/api/greenroom")
                        .file(image)
                        .file(data)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer "+token));
    }

    private RestDocumentationResultHandler getDocumentForGreenroomRegistration(Integer identifier){
        return document("api/greenroom/post/"+identifier,
                preprocessRequest(prettyPrint(),modifyUris().scheme("https").host("greenroom-server.site").removePort()),   // (2)
                preprocessResponse(prettyPrint(), getModifiedHeader()),  // (3)
                requestParts(requestPartDescriptorsForGreenroomRegistration),
                requestPartFields("data",requestPartFieldDescriptorsForGreenroomRegistration),
                responseFields(resultDescriptorsForLevelUp), // responseBody 설명
                requestHeaders(headerWithName("Authorization").description("Bearer : 사용자 access Token")),
                resource(
                        ResourceSnippetParameters.builder()
                                .tag("그린룸") // 문서에서 api들이 태그로 분류됨
                                .summary("그린룸 등록 api") // api 이름
                                .description("그린룸 등록") // api 설명
                                .build()));
    }

    @Test
    @Transactional
    public void 그린룸_생성_성공() throws Exception {
        //given

        //test용 data 생성
        signupForTest();
        GreenroomRegistrationRequestDto greenroomRegistrationRequestDto = new GreenroomRegistrationRequestDto(1L,"초롱이","2025-02-25",10,1L);
        MockMultipartFile image = getTestMultiPartFile();
        MockMultipartFile data = new MockMultipartFile("data", "", "application/json", mapper.writeValueAsString(greenroomRegistrationRequestDto).getBytes());

        //when
         ResultActions resultActions = getResultActionsForGreenroomRegistration(image,data);

        //then
        resultActions.andExpect(status().isCreated());

        //문서화
        resultActions.andDo(getDocumentForGreenroomRegistration(1));

    }


    @Test
    @Transactional
    public void 그린룸_생성_성공2() throws Exception {
        //given

        //test용 data 생성
        User user = signupForTest();
        user.updateIsFirstGreenroomRegistered(true);
        GreenroomRegistrationRequestDto greenroomRegistrationRequestDto = new GreenroomRegistrationRequestDto(1L,"초롱이","2025-02-25",10,1L);
        MockMultipartFile image = getTestMultiPartFile();
        MockMultipartFile data = new MockMultipartFile("data", "", "application/json", mapper.writeValueAsString(greenroomRegistrationRequestDto).getBytes());

        //when
        ResultActions resultActions = getResultActionsForGreenroomRegistration(image,data);

        //then
        resultActions.andExpect(status().isCreated());

        //문서화
        resultActions.andDo(getDocumentForGreenroomRegistration(2));

    }


    @Test
    @Transactional
    public void 그린룸_생성_실패1() throws Exception {
        //given
        signupForTest();
        GreenroomRegistrationRequestDto greenroomRegistrationRequestDto = new GreenroomRegistrationRequestDto(100000000L,"초롱이","2025-02-25",10,1L);
        MockMultipartFile image = getTestMultiPartFile();
        MockMultipartFile data = new MockMultipartFile("data", "", "application/json", mapper.writeValueAsString(greenroomRegistrationRequestDto).getBytes());

        //when
        ResultActions resultActions = getResultActionsForGreenroomRegistration(image,data);

        //then
        resultActions.andExpect(status().is(ResponseCodeEnum.PLANT_NOT_FOUND.getStatus().value())).andExpect(jsonPath("code").value(ResponseCodeEnum.PLANT_NOT_FOUND.getCode()));

        //문서화
        resultActions.andDo(getDocumentForGreenroomRegistration(3));
    }

    @Test
    @Transactional
    public void 그린룸_생성_실패2() throws Exception {
        //given
        signupForTest();
        GreenroomRegistrationRequestDto greenroomRegistrationRequestDto = new GreenroomRegistrationRequestDto(1L,"초롱이","2025-02-25",10,1000000000L);
        MockMultipartFile image = getTestMultiPartFile();
        MockMultipartFile data = new MockMultipartFile("data", "", "application/json", mapper.writeValueAsString(greenroomRegistrationRequestDto).getBytes());

        //when
        ResultActions resultActions = getResultActionsForGreenroomRegistration(image,data);

        //then
        resultActions.andExpect(status().is(ResponseCodeEnum.ITEM_NOT_FOUND.getStatus().value())).andExpect(jsonPath("code").value(ResponseCodeEnum.ITEM_NOT_FOUND.getCode()));

        //문서화
        resultActions.andDo(getDocumentForGreenroomRegistration(4));
    }

    @Test
    @Transactional
    public void 그린룸_생성_실패3() throws Exception {
        //given

        //test 용 data
        signupForTest();
        GreenroomRegistrationRequestDto greenroomRegistrationRequestDto = new GreenroomRegistrationRequestDto(1L,"초롱이","2025-02-25",10,1L);
        MockMultipartFile image = getInvalidTestMultiPartFile();
        MockMultipartFile data = new MockMultipartFile("data", "", "application/json", mapper.writeValueAsString(greenroomRegistrationRequestDto).getBytes());

        //when
        ResultActions resultActions = getResultActionsForGreenroomRegistration(image,data);

        //then
        resultActions.andExpect(status().is(ResponseCodeEnum.INVALID_IMAGE_FORMAT.getStatus().value())).andExpect(jsonPath("code").value(ResponseCodeEnum.INVALID_IMAGE_FORMAT.getCode()));

        //문서화
        resultActions.andDo(getDocumentForGreenroomRegistration(5));
    }

    @Test
    @Transactional
    public void 그린룸_생성_실패4() throws Exception {
        //given
        //test 용 data
        signupForTest();
        GreenroomRegistrationRequestDto greenroomRegistrationRequestDto = new GreenroomRegistrationRequestDto(1L,"초롱이","2025-02-25",10,1L);
        MockMultipartFile image = getTestMultiPartFile();
        MockMultipartFile data = new MockMultipartFile("data", "", "application/json", mapper.writeValueAsString(greenroomRegistrationRequestDto).getBytes());

        //when
        doThrow(new CustomException(ResponseCodeEnum.FAIL_TO_UPLOAD_IMAGE)).when(mockitoGreenroomService).createGreenroom(EMAIL,greenroomRegistrationRequestDto,image);
        ResultActions resultActions = getResultActionsForGreenroomRegistration(image,data);

        //then
        resultActions.andExpect(status().is(ResponseCodeEnum.FAIL_TO_UPLOAD_IMAGE.getStatus().value())).andExpect(jsonPath("code").value(ResponseCodeEnum.FAIL_TO_UPLOAD_IMAGE.getCode()));

        //문서화
        resultActions.andDo(getDocumentForGreenroomRegistration(6));
    }


    @Test
    @Transactional
    public void 할일_완료_성공() throws Exception {
        //given
        User user = signupForTest();
        GreenRoom greenRoom = createGreenRoom(user);
        String token = getTokenForTest((long) (1000*18));

        CompleteTodoRequestDto completeTodoRequestDto = new CompleteTodoRequestDto(List.of(1L));

        //when
        ResultActions resultActions = mockMvc.perform(
                RestDocumentationRequestBuilders
                        .post("/api/greenroom/{greenroom_id}/todo/completion",greenRoom.getGreenroomId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer "+token)
                        .content(mapper.writeValueAsString(completeTodoRequestDto))
                        .contentType(MediaType.APPLICATION_JSON)

        );

        //then
        resultActions.andExpect(status().isOk());

        //문서화
        resultActions.andDo(document("api/greenroom/todo/completion/"+1,
                preprocessRequest(prettyPrint(),modifyUris().scheme("https").host("greenroom-server.site").removePort()),   // (2)
                preprocessResponse(prettyPrint(), getModifiedHeader()),  // (3)
                requestHeaders(headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer : 사용자 access Token")),
                requestFields(requestBodyDescriptorsForCompleteTodo),
                responseFields(resultDescriptorsForLevelUp),
                pathParameters(pathParameterForGreenroomId),
                resource(
                        ResourceSnippetParameters.builder()
                                .tag("그린룸") // 문서에서 api들이 태그로 분류됨
                                .summary("할 일 완료 처리 api") // api 이름
                                .description("사용자가 키우는 식물에 대한 할 일 완료 처리 api") // api 설명
                                .build())));

    }

    @Test
    @Transactional
    public void 할일_완료_실패() throws Exception {
        //given
        User user = signupForTest();
        GreenRoom greenRoom = createGreenRoom(user);
        String token = getTokenForTest((long) (1000*18));

        CompleteTodoRequestDto completeTodoRequestDto = new CompleteTodoRequestDto(List.of(1L));


        //when
        ResultActions resultActions = mockMvc.perform(
                RestDocumentationRequestBuilders
                        .post("/api/greenroom/{greenroom_id}/todo/completion",10)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer "+token)
                        .content(mapper.writeValueAsString(completeTodoRequestDto))
                        .contentType(MediaType.APPLICATION_JSON)
        );

        //then
        resultActions.andExpect(status().is(ResponseCodeEnum.GREENROOM_NOT_FOUND.getStatus().value())).andExpect(jsonPath("code").value(ResponseCodeEnum.GREENROOM_NOT_FOUND.getCode()));

        //문서화
        resultActions.andDo(document("api/greenroom/todo/completion/"+2,
                preprocessRequest(prettyPrint(),modifyUris().scheme("https").host("greenroom-server.site").removePort()),   // (2)
                preprocessResponse(prettyPrint(), getModifiedHeader()),  // (3)
                requestHeaders(headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer : 사용자 access Token")),
                responseFields(resultDescriptorsForLevelUp),
                requestFields(requestBodyDescriptorsForCompleteTodo),
                pathParameters(pathParameterForGreenroomId),
                resource(
                        ResourceSnippetParameters.builder()
                                .tag("그린룸") // 문서에서 api들이 태그로 분류됨
                                .summary("할 일 완료 처리 api") // api 이름
                                .description("사용자가 키우는 식물에 대한 할 일 완료 처리 api") // api 설명
                                .build())));
    }

    private ResultActions getResultActionsForAdornment(GreenroomDecorationRequestDto greenroomDecorationRequestDto, Long greenroomId) throws Exception {

        String token = getTokenForTest((long) (10*1000));
        return mockMvc.perform( // api 실행
                RestDocumentationRequestBuilders
                        .post("/api/greenroom/{greenroom_id}/items",greenroomId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(greenroomDecorationRequestDto))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer "+token));
    }

    private RestDocumentationResultHandler getDocumentForAdornment(Integer identifier){
        return document("api/greenroom/items/"+identifier,
                preprocessRequest(prettyPrint(),modifyUris().scheme("https").host("greenroom-server.site").removePort()),   // (2)
                preprocessResponse(prettyPrint(), getModifiedHeader()),  // (3)
                requestFields(requestBodyDescriptorsForAdornment),
                responseFields(resultDescriptorsForAdornment), // responseBody 설명
                requestHeaders(headerWithName("Authorization").description("Bearer : 사용자 access Token")),
                resource(
                        ResourceSnippetParameters.builder()
                                .tag("그린룸") // 문서에서 api들이 태그로 분류됨
                                .summary("그린룸 꾸미기 api") // api 이름
                                .description("그린룸 item을 등록함.") // api 설명
                                .build()));
    }



    @Test
    @Transactional
    public void 그린룸_꾸미기_성공() throws Exception {
        //given
        User user = signupForTest();
        GreenRoom greenRoom = createGreenRoom(user);
        GreenroomDecorationRequestDto greenroomDecorationRequestDto = new GreenroomDecorationRequestDto(1L,25L,24L,35L,null);

        //when
        ResultActions resultActions = getResultActionsForAdornment(greenroomDecorationRequestDto,greenRoom.getGreenroomId());

        //then
        resultActions.andExpect(status().isOk());

        //문서화
        resultActions.andDo(getDocumentForAdornment(1));

    }

    @Test
    @Transactional
    public void 그린룸_꾸미기_실패1() throws Exception {
        //given
        User user = signupForTest();
        GreenRoom greenRoom =  createGreenRoom(user);
        GreenroomDecorationRequestDto greenroomDecorationRequestDto = new GreenroomDecorationRequestDto(1L,25L,24L,100L,null);

        //when
        ResultActions resultActions = getResultActionsForAdornment(greenroomDecorationRequestDto,greenRoom.getGreenroomId());

        //then
        resultActions.andExpect(status().is(ResponseCodeEnum.ITEM_NOT_FOUND.getStatus().value())).andExpect(jsonPath("code").value(ResponseCodeEnum.ITEM_NOT_FOUND.getCode()));

        //문서화
        resultActions.andDo(getDocumentForAdornment(2));

    }


    @Test
    @Transactional
    public void 그린룸_꾸미기_실패2() throws Exception {
        //given
        User user = signupForTest();
        GreenroomDecorationRequestDto greenroomDecorationRequestDto = new GreenroomDecorationRequestDto(1L,23L,24L,35L,null);

        //when
        ResultActions resultActions = getResultActionsForAdornment(greenroomDecorationRequestDto,100L);

        //then
        resultActions.andExpect(status().is(ResponseCodeEnum.GREENROOM_NOT_FOUND.getStatus().value())).andExpect(jsonPath("code").value(ResponseCodeEnum.GREENROOM_NOT_FOUND.getCode()));

        //문서화
        resultActions.andDo(getDocumentForAdornment(3));

    }

    @Test
    @Transactional
    public void 그린룸_꾸미기_실패3() throws Exception {
        //given
        User user = signupForTest();
        GreenRoom greenRoom =  createGreenRoom(user);
        GreenroomDecorationRequestDto greenroomDecorationRequestDto = new GreenroomDecorationRequestDto(1L,23L,24L,35L,null);

        //when
        ResultActions resultActions = getResultActionsForAdornment(greenroomDecorationRequestDto,greenRoom.getGreenroomId());

        //then
        resultActions.andExpect(status().is(ResponseCodeEnum.INVALID_REQUEST_ARGUMENT.getStatus().value())).andExpect(jsonPath("code").value(ResponseCodeEnum.INVALID_REQUEST_ARGUMENT.getCode()));

        //문서화
        resultActions.andDo(getDocumentForAdornment(4));

    }

    private ResultActions getResultActionsForGreenroomDetails(Long greenroomId) throws Exception {

        String token = getTokenForTest((long) (10*1000));
        return mockMvc.perform( // api 실행
                RestDocumentationRequestBuilders
                        .get("/api/greenroom/{greenroom_id}/details",greenroomId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer "+token));
    }

    private RestDocumentationResultHandler getDocumentForGreenroomDetails(Integer identifier){
        return document("api/greenroom/details/"+identifier,
                preprocessRequest(prettyPrint(),modifyUris().scheme("https").host("greenroom-server.site").removePort()),   // (2)
                preprocessResponse(prettyPrint(), getModifiedHeader()),  // (3)
                responseFields(resultDescriptorsForGreenroomDetails), // responseBody 설명
                requestHeaders(headerWithName("Authorization").description("Bearer : 사용자 access Token")),
                pathParameters(pathParameterForGreenroomId),
                resource(
                        ResourceSnippetParameters.builder()
                                .tag("그린룸") // 문서에서 api들이 태그로 분류됨
                                .summary("그린룸 상세 정보 조회 api") // api 이름
                                .description("그린룸의 상세 정보를 조회함.") // api 설명
                                .build()));
    }

    @Test
    @Transactional
    public void 그린룸_상세정보조회_성공() throws Exception {
        //given
        User user = signupForTest();
        GreenRoom greenRoom =  createGreenRoom(user);
        greenRoom.updateCreationDate(LocalDateTime.now().minusDays(3));

        //when
        ResultActions resultActions = getResultActionsForGreenroomDetails(greenRoom.getGreenroomId());

        //then
        resultActions.andExpect(status().isOk());

        //문서화
        resultActions.andDo(getDocumentForGreenroomDetails(1));
    }

    @Test
    @Transactional
    public void 그린룸_상세정보조회_실패() throws Exception {
        //given

        //when
        ResultActions resultActions = getResultActionsForGreenroomDetails(100L);

        //then
        resultActions.andExpect(status().is(ResponseCodeEnum.GREENROOM_NOT_FOUND.getStatus().value())).andExpect(jsonPath("code").value(ResponseCodeEnum.GREENROOM_NOT_FOUND.getCode()));

        //문서화
        resultActions.andDo(getDocumentForGreenroomDetails(2));
    }
}
