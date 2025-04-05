package com.greenroom.server.api;

import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.greenroom.server.api.config.TestExecutionListener;
import com.greenroom.server.api.domain.greenroom.dto.in.*;
import com.greenroom.server.api.domain.greenroom.entity.*;
import com.greenroom.server.api.domain.greenroom.enums.GreenRoomStatus;
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
import org.springframework.http.HttpStatus;
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

    @Autowired
    private TodoLogRepository todoLogRepository;

    @Autowired
    private DiaryRepository diaryRepository;

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

    public User signupForTest2(){
        User user = User.createUser(new SignupRequestDto("test@gmail.com",PW,NAME), gradeRepository.findById(1L).orElse(null));
        userRepository.save(user);
        return user;
    }

    public GreenRoom createGreenRoom(User user){

        Plant plant = plantRepository.findById(20L).get();


        GreenRoom greenRoom = GreenRoom.of("test 그린룸",null,user,plant);

        greenRoomRepository.save(greenRoom);

        Todo todo = Todo.builder().greenRoom(greenRoom).activity(activityRepository.findAll().get(0)).nextTodoDate(LocalDate.now().plusDays(8)).term(10).baseDate(LocalDate.now().minusDays(2)).build();
        todoRepository.save(todo);

        adornmentRepository.save(Adornment.createAdornment(itemRepository.findAll().get(0),greenRoom));


        return greenRoom;
    }

    public GreenRoom createGreenRoom2(User user){

        Plant plant = plantRepository.findById(21L).get();

        GreenRoom greenRoom = GreenRoom.of("test그린룸22",null,user,plant);

        greenRoomRepository.save(greenRoom);

        Todo todo = Todo.builder().greenRoom(greenRoom).activity(activityRepository.findAll().get(0)).nextTodoDate(LocalDate.now().plusDays(8)).term(10).baseDate(LocalDate.now().minusDays(2)).build();
        todoRepository.save(todo);

        adornmentRepository.save(Adornment.createAdornment(itemRepository.findAll().get(0),greenRoom));

        return greenRoom;
    }

    public GreenRoom createGreenRoomForCalender(User user){

        Plant plant = plantRepository.findById(20L).get();

        GreenRoom greenRoom = GreenRoom.of("초롱이",null,user,plant);
        greenRoomRepository.save(greenRoom);

        Todo todo1 = Todo.builder().greenRoom(greenRoom).activity(activityRepository.findAll().get(0)).nextTodoDate(LocalDate.now().minusDays(2)).term(5).baseDate(LocalDate.now().minusDays(7)).build();
        Todo todo2 = Todo.builder().greenRoom(greenRoom).activity(activityRepository.findAll().get(2)).nextTodoDate(LocalDate.now().plusDays(2)).term(5).baseDate(LocalDate.now().minusDays(3)).build();
        TodoLog todoLog = TodoLog.builder().greenRoom(greenRoom).activity(activityRepository.findAll().get(0)).build();
        todoLogRepository.save(todoLog);

        todoRepository.save(todo1);todoRepository.save(todo2);

        Diary diary = Diary.builder().greenRoom(greenRoom).title("제목입니다~").content("본문입니다 ~ ").diaryPictureUrl(null).build();
        diaryRepository.save(diary);

        GreenRoom greenRoom2 = GreenRoom.of("아롱이",null,user,plant);
        greenRoom2.updateStatus(GreenRoomStatus.DISABLED);
        greenRoomRepository.save(greenRoom2);

        Todo todo3 = Todo.builder().greenRoom(greenRoom2).activity(activityRepository.findAll().get(0)).nextTodoDate(LocalDate.now().minusDays(2)).term(5).baseDate(LocalDate.now().minusDays(7)).build();
        Todo todo4 = Todo.builder().greenRoom(greenRoom2).activity(activityRepository.findAll().get(2)).nextTodoDate(LocalDate.now().plusDays(2)).term(5).baseDate(LocalDate.now().minusDays(3)).build();
        TodoLog todoLog2 = TodoLog.builder().greenRoom(greenRoom2).activity(activityRepository.findAll().get(0)).build();
        todoLogRepository.save(todoLog2);

        todoRepository.save(todo3);todoRepository.save(todo4);

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

    private final List<ParameterDescriptor> queryParametersForDiaryList = List.of(
            parameterWithName("date").description("일기를 조회할 날짜(연도-월) : YYYY-MM")
    );

    private final List<ParameterDescriptor> pathParameterForGreenroomId = List.of(
        parameterWithName("greenroom_id").description("그린룸 id")
    );

    private final List<ParameterDescriptor> pathParameterForDiary = List.of(
            parameterWithName("diary_id").description("일기 고유 id")
    );

    List<RequestPartDescriptor> requestPartDescriptorsForGreenroomRegistration = List.of(
            partWithName("imageFile").description("식물 이미지 파일").attributes(new Attributes.Attribute("content-type","image/*")),
            partWithName("data").description("그린룸 등록 정보").attributes(new Attributes.Attribute("content-type","application/json"))
    );

    List<FieldDescriptor> requestPartFieldDescriptorsForGreenroomRegistration = List.of(
            fieldWithPath("plantId").type(JsonFieldType.NUMBER).description("식물 종류 id").optional().attributes(new Attributes.Attribute("constraint",
                    """
                            식물을 등록하지 않을 경우 +
                            1. null +
                            2. request body에서 필드 제외""")),
            fieldWithPath("nickname").type(JsonFieldType.STRING).description("식물 별명"),
            fieldWithPath("wateringBaseDate").type(JsonFieldType.STRING).description("물주기 기준 날짜").attributes(new Attributes.Attribute("constraint","YYYY-MM-DD")),
            fieldWithPath("wateringInterval").type(JsonFieldType.NUMBER).description("물 주는 주기"),
            fieldWithPath("itemId").type(JsonFieldType.NUMBER).description("식물 형태 id")
    );


    List<RequestPartDescriptor> requestPartDescriptorsForGreenroomInfoUpdate = List.of(
            partWithName("imageFile").description("이미지 파일. \n+ 이미지를 등록/갱신하는 경우에만 첨부").attributes(new Attributes.Attribute("content-type","image/*")),
            partWithName("data").description("그린룸 변경 정보").attributes(new Attributes.Attribute("content-type","application/json"))
    );

    List<FieldDescriptor> requestPartFieldDescriptorsForGreenroomInfoUpdate = List.of(
            fieldWithPath("nickname").type(JsonFieldType.STRING).description("그린룸 별명"),
            fieldWithPath("imageAction").type(JsonFieldType.STRING).description("이미지 처리 방법").attributes(new Attributes.Attribute("constraint","""
                            이미지 처리 방법 +
                            1. NONE : 변경하지 않음 +
                            2. UPLOAD : 이미지 등록/변경 +
                            3. DELETE : 이미지를 삭제함
                            """)),
            fieldWithPath("isAlive").type(JsonFieldType.BOOLEAN).description("식물 상태. 여전히 키우고 있으면 true, 죽었으면 false")
    );

    List<RequestPartDescriptor> requestPartDescriptorsForDiaryCreation = List.of(
            partWithName("imageFile").description("일기 이미지 파일").attributes(new Attributes.Attribute("content-type","image/*")).optional(),
            partWithName("data").description("일기 작성 정보").attributes(new Attributes.Attribute("content-type","application/json"))
    );

    List<FieldDescriptor> requestPartFieldDescriptorsForDiaryCreation = List.of(
            fieldWithPath("title").type(JsonFieldType.STRING).description("일기 제목").attributes(new Attributes.Attribute("constraint","1자 이상 65자 이하")),
            fieldWithPath("content").type(JsonFieldType.STRING).description("일기 본문").attributes(new Attributes.Attribute("constraint","1자 이상 500자 이하")),
            fieldWithPath("date").type(JsonFieldType.STRING).description("물주기 기준 날짜").attributes(new Attributes.Attribute("constraint","YYYY-MM-DD"))
    );


    List<FieldDescriptor> requestBodyDescriptorsForCompleteTodo = List.of(
            fieldWithPath("completedTodo").type(JsonFieldType.ARRAY).description("완료 처리할 작업의 id 목록")
    );


    List<FieldDescriptor> requestBodyDescriptorsForAdornment = List.of(
            fieldWithPath("shape").type(JsonFieldType.NUMBER).description("식물 형태 item id"),
            fieldWithPath("eyewear").type(JsonFieldType.NUMBER).description("안경 악세서리 item id").optional().attributes(new Attributes.Attribute("constraint",
                    """
                            item을 사용하지 않는 경우 +
                            1. null +
                            2. request body에서 필드 제외""")),
            fieldWithPath("hairAccessory").type(JsonFieldType.NUMBER).description("헤어핀 악세서리 item id").optional().attributes(new Attributes.Attribute("constraint",
                    """
                            item을 사용하지 않는 경우 +
                            1. null +
                            2. request body에서 필드 제외""")),
            fieldWithPath("shelf").type(JsonFieldType.NUMBER).description("선반 소품 item id").optional().attributes(new Attributes.Attribute("constraint",
                    """
                            item을 사용하지 않는 경우 +
                            1. null +
                            2. request body에서 필드 제외""")),
            fieldWithPath("window").type(JsonFieldType.NUMBER).description("창문 소품 item id").optional().attributes(new Attributes.Attribute("constraint",
                    """
                            item을 사용하지 않는 경우 +
                            1. null +
                            2. request body에서 필드 제외"""))
    );

    List<FieldDescriptor> requestBodyDescriptorsForGreenroomMemo =List.of(
            fieldWithPath("memo").type(JsonFieldType.STRING).description("메모").optional().attributes(new Attributes.Attribute("constraint",
                    """
                            30자 제한 +
                            메모를 등록하지 않을 경우 +
                            1. null +
                            2. 공백("") +
                            2. request body에서 필드 제외""")));

    List<FieldDescriptor> requestBodyDescriptorsForGreenroomPlant =List.of(
            fieldWithPath("plantId").type(JsonFieldType.NUMBER).description("등록하고자 하는 식물의 고유 id").optional().attributes(new Attributes.Attribute("constraint",
                    """
                            식물을 등록하지 않을 경우 +
                            1. null +
                            2. request body에서 필드 제외""")));
    List<FieldDescriptor> requestBodyDescriptorsForUpdateActivityStatus =List.of(
            fieldWithPath("activeList").type(JsonFieldType.ARRAY).description("활성화할 주기의 activity id").attributes(new Attributes.Attribute("constraint","전달된 주기에 대해서만 상태 변경을 요청함. 전달되지 않은 주기는 이전 상태 값을 유지함.")),
            fieldWithPath("inactiveList").type(JsonFieldType.ARRAY).description("비활성화할 주기의 activity id").attributes(new Attributes.Attribute("constraint","전달된 주기에 대해서만 상태 변경을 요청함. 전달되지 않은 주기는 이전 상태 값을 유지함.")));

    List<FieldDescriptor> requestBodyDescriptorsForUpdateActivityInfo =List.of(
            fieldWithPath("updateList").type(JsonFieldType.ARRAY).description("주기 변경 정보 list"),
            fieldWithPath("updateList[].activityId").type(JsonFieldType.NUMBER).description("정보를 변경할 주기의 activity id"),
            fieldWithPath("updateList[].date").type(JsonFieldType.STRING).description("기준 날짜").attributes(new Attributes.Attribute("constraint", """ 
                    형태 : YYYY-MM-DD +
                    제한 : 미래 날짜는 불가능함
                    """
            )),
            fieldWithPath("updateList[].term").type(JsonFieldType.NUMBER).description("간격"));

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
            fieldWithPath("data").type(JsonFieldType.OBJECT).optional().description("data").optional(),
            fieldWithPath("data.basicInfo").type(JsonFieldType.OBJECT).optional().description("그린룸 기본 정보"),
            fieldWithPath("data.basicInfo.greenroomId").type(JsonFieldType.NUMBER).description("그린룸 고유 id"),
            fieldWithPath("data.basicInfo.nickName").type(JsonFieldType.STRING).description("그린룸 이름"),
            fieldWithPath("data.basicInfo.plantName").type(JsonFieldType.STRING).description("식물 이름").optional().attributes(new Attributes.Attribute("constraint","등록된 식물이 없으면 null")),
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
            fieldWithPath("data.plantInfo").type(JsonFieldType.OBJECT).description("식물 기본 정보").optional().attributes(new Attributes.Attribute("constraint","등록된 식물이 없으면 null")),
            fieldWithPath("data.plantInfo.plantId").type(JsonFieldType.NUMBER).description("식물 id").optional(),
            fieldWithPath("data.plantInfo.name").type(JsonFieldType.STRING).description("식물 이름").optional(),
            fieldWithPath("data.plantInfo.scientificName").type(JsonFieldType.STRING).description("식물 학명").optional(),
            fieldWithPath("data.plantInfo.description").type(JsonFieldType.STRING).description("식물에 대한 설명").optional(),
            fieldWithPath("data.plantInfo.imageUrl").type(JsonFieldType.STRING).description("식물 사진").optional(),
            fieldWithPath("data.plantManagementInfo").type(JsonFieldType.OBJECT).description("식물 키우는 법").optional().attributes(new Attributes.Attribute("constraint","등록된 식물이 없으면 null")),
            fieldWithPath("data.plantManagementInfo.managementLevel").type(JsonFieldType.STRING).description("관리 레벨 정보").optional(),
            fieldWithPath("data.plantManagementInfo.temperature").type(JsonFieldType.STRING).description("온도 정보").optional(),
            fieldWithPath("data.plantManagementInfo.sunlight").type(JsonFieldType.STRING).description("햇빛 정보").optional(),
            fieldWithPath("data.plantManagementInfo.watering").type(JsonFieldType.STRING).description("물주기 정보").optional(),
            fieldWithPath("data.plantManagementInfo.humidity").type(JsonFieldType.STRING).description("습도 정보").optional(),
            fieldWithPath("data.plantManagementInfo.fertilizer").type(JsonFieldType.STRING).description("비료 정보").optional()
            );

    List<FieldDescriptor> resultDescriptorsForGetActivityInfo= List.of(
            fieldWithPath("status").type(JsonFieldType.STRING).description("응답 상태"),
            fieldWithPath("code").type(JsonFieldType.STRING).description("상태 코드"),
            fieldWithPath("data").type(JsonFieldType.OBJECT).optional().description("data").optional(),
            fieldWithPath("data.activeCycle").type(JsonFieldType.ARRAY).description("활성화된 주기 정보").attributes(new Attributes.Attribute("constraint","활성화된 주기가 없을 경우 빈 배열 반환")),
            fieldWithPath("data.activeCycle[].activityId").type(JsonFieldType.NUMBER).description("activity id : activity id 문서 부분 참조"),
            fieldWithPath("data.activeCycle[].activityName").type(JsonFieldType.STRING).description("activity 이름"),
            fieldWithPath("data.activeCycle[].lastDate").type(JsonFieldType.STRING).optional().description("마지막으로 수행한 기준 날짜").attributes(new Attributes.Attribute("constraint","처음으로 주기를 활성화한 경우, 기준 날짜가 null로 반환됨.")),
            fieldWithPath("data.activeCycle[].term").type(JsonFieldType.NUMBER).optional().description("행동 주기").attributes(new Attributes.Attribute("constraint","처음으로 주기를 활성화한 경우, 행동 주기 간격이 null로 반환됨.")),
            fieldWithPath("data.inactiveCycle").type(JsonFieldType.ARRAY).description("비활성화된 주기 정보").attributes(new Attributes.Attribute("constraint","비활성화된 주기가 없을 경우 빈 배열 반환")),
            fieldWithPath("data.inactiveCycle[].activityId").type(JsonFieldType.NUMBER).description("activity id : activity id 문서 부분 참조"),
            fieldWithPath("data.inactiveCycle[].activityName").type(JsonFieldType.STRING).description("activity 이름"));

    List<FieldDescriptor> resultDescriptorsForCalenderInfo= List.of(
            fieldWithPath("status").type(JsonFieldType.STRING).description("응답 상태"),
            fieldWithPath("code").type(JsonFieldType.STRING).description("상태 코드"),
            fieldWithPath("data").type(JsonFieldType.OBJECT).optional().description("data").optional(),
            fieldWithPath("data.dateInfo").type(JsonFieldType.OBJECT).description("조회 날짜 정보"),
            fieldWithPath("data.dateInfo.year").type(JsonFieldType.NUMBER).description("조회 날짜 연도"),
            fieldWithPath("data.dateInfo.month").type(JsonFieldType.NUMBER).description("조회 날짜 월"),
            fieldWithPath("data.dateInfo.date").type(JsonFieldType.NUMBER).description("조회 날짜 일자"),
            fieldWithPath("data.dateInfo.day").type(JsonFieldType.STRING).description("조회 날짜 요일"),
            fieldWithPath("data.mainInfo").type(JsonFieldType.ARRAY).description("활동 정보 및 일기 내역").optional().attributes(new Attributes.Attribute("constraint","정보를 조회할 그린룸이 없거나 활동 내역이 없는 경우 빈 배열")),
            fieldWithPath("data.mainInfo[].greenroomInfo").type(JsonFieldType.OBJECT).description("그린룸 정보"),
            fieldWithPath("data.mainInfo[].greenroomInfo.greenroomId").type(JsonFieldType.NUMBER).description("그린룸 id"),
            fieldWithPath("data.mainInfo[].greenroomInfo.greenroomName").type(JsonFieldType.STRING).description("그린룸 닉네임"),

            fieldWithPath("data.mainInfo[].activity").type(JsonFieldType.ARRAY).description("활동 정보 내역").optional().attributes(new Attributes.Attribute("constraint","활동 내역 정보가 없는 경우 빈 배열")),
            fieldWithPath("data.mainInfo[].activity[].activityId").type(JsonFieldType.NUMBER).description("activity id"),
            fieldWithPath("data.mainInfo[].activity[].activityName").type(JsonFieldType.STRING).description("activity 이름"),
            fieldWithPath("data.mainInfo[].activity[].isCompleted").type(JsonFieldType.BOOLEAN).description("""
                           활동 완료 여부:  +
                            1. 과거 날짜 조회시(이미 완료함) : true +
                            2. 현재 날짜 조회시 : 오늘 해야할 일을 완료 했을 경우 true, 미완료시 false +
                            3. 미래 날짜 조회시(아직 미완료) : false """),
            fieldWithPath("data.mainInfo[].activity[].remainingDays").type(JsonFieldType.NUMBER).optional().description(
                    """
                            현재 날짜를 기준으로 활동 수행 날짜까지 남은 일수 +
                            1. 이미 완료한 경우 : null +
                            2. 수행해야 하는 날짜가 현재 날짜보다 과거인 경우 : -값 +
                            3. 수행해야 하는 날짜가 오늘인 경우 : 0 +
                            4. 수행해야 하는 날짜가 현재 날짜보다 미래인 경우 : +값
                            
                            """
            ).attributes(new Attributes.Attribute("constraint","과거 날짜 조회시 이미 완료된 활동 내역이므로 null")),
            fieldWithPath("data.mainInfo[].diary").type(JsonFieldType.ARRAY).description("활동 정보 및 일기 내역").optional().attributes(new Attributes.Attribute("constraint","일기 내역이 없는 경우 빈 배열")),
            fieldWithPath("data.mainInfo[].diary[].diaryId").type(JsonFieldType.NUMBER).description("일기 id"),
            fieldWithPath("data.mainInfo[].diary[].greenroomId").type(JsonFieldType.NUMBER).description("그린룸 id"),
            fieldWithPath("data.mainInfo[].diary[].greenroomName").type(JsonFieldType.STRING).description("그린룸 별명"),
            fieldWithPath("data.mainInfo[].diary[].title").type(JsonFieldType.STRING).description("일기 제목"),
            fieldWithPath("data.mainInfo[].diary[].body").type(JsonFieldType.STRING).description("일기 본문"),
            fieldWithPath("data.mainInfo[].diary[].imageUrl").type(JsonFieldType.STRING).description("일기에 등록한 이미지 url").optional().attributes(new Attributes.Attribute("constraint","등록한 이미지가 없을 경우 null")),
            fieldWithPath("data.mainInfo[].diary[].dateTime").type(JsonFieldType.STRING).description("일기 작성 날짜 및 시간 : YYYY-MM-DDTHH:MM:SS"));


    List<FieldDescriptor> resultDescriptorsForDiaryCreation = List.of(
            fieldWithPath("status").type(JsonFieldType.STRING).description("응답 상태"),
            fieldWithPath("code").type(JsonFieldType.STRING).description("상태 코드"),
            fieldWithPath("data").type(JsonFieldType.OBJECT).optional().description("data"),
            fieldWithPath("data.diaryId").type(JsonFieldType.NUMBER).description("새롭게 생성된 다이어리 고유 id"),
            fieldWithPath("data.greenroomId").type(JsonFieldType.NUMBER).description("그린룸 id"),
            fieldWithPath("data.greenroomName").type(JsonFieldType.STRING).description("그린룸 별명"),
            fieldWithPath("data.title").type(JsonFieldType.STRING).description("일기 제목"),
            fieldWithPath("data.content").type(JsonFieldType.STRING).description("일기 본문"),
            fieldWithPath("data.imageUrl").type(JsonFieldType.STRING).description("일기 이미지").optional().attributes(new Attributes.Attribute("constraint","등록된 이미지가 없으면 null")),
            fieldWithPath("data.dateTime").type(JsonFieldType.STRING).description("일기 작성 날짜 및 시간 : YYYY-MM-DDTHH:MM:SS")
    );

    List<FieldDescriptor> resultDescriptorsForDiaryList = List.of(
            fieldWithPath("status").type(JsonFieldType.STRING).description("응답 상태"),
            fieldWithPath("code").type(JsonFieldType.STRING).description("상태 코드"),
            fieldWithPath("data").type(JsonFieldType.OBJECT).optional().description("data"),
            fieldWithPath("data.diaryList").type(JsonFieldType.ARRAY).optional().description("날짜 별 일기 목록").attributes(new Attributes.Attribute("constraint","해당 날짜(연도+월)에 등록된 일기가 없으면 빈 배열 반환")),
            fieldWithPath("data.diaryList[].dateInfo").type(JsonFieldType.OBJECT).description("일기 작성된 날짜"),
            fieldWithPath("data.diaryList[].dateInfo.year").type(JsonFieldType.NUMBER).description("일기 작성 연도"),
            fieldWithPath("data.diaryList[].dateInfo.month").type(JsonFieldType.NUMBER).description("일기 작성 월"),
            fieldWithPath("data.diaryList[].dateInfo.date").type(JsonFieldType.NUMBER).description("일기 작성 일자"),
            fieldWithPath("data.diaryList[].dateInfo.day").type(JsonFieldType.STRING).description("일기 작성 요일"),
            fieldWithPath("data.diaryList[].diaryInfo").type(JsonFieldType.ARRAY).description("날짜 별 작성된 일기 목록"),
            fieldWithPath("data.diaryList[].diaryInfo[].diaryId").type(JsonFieldType.NUMBER).description("일기 id"),
            fieldWithPath("data.diaryList[].diaryInfo[].greenroomId").type(JsonFieldType.NUMBER).description("그린룸 id"),
            fieldWithPath("data.diaryList[].diaryInfo[].greenroomName").type(JsonFieldType.STRING).description("그린룸 이름"),
            fieldWithPath("data.diaryList[].diaryInfo[].title").type(JsonFieldType.STRING).description("일기 제목"),
            fieldWithPath("data.diaryList[].diaryInfo[].content").type(JsonFieldType.STRING).description("일기 본문"),
            fieldWithPath("data.diaryList[].diaryInfo[].imageUrl").type(JsonFieldType.STRING).description("일기 이미지 url").optional().attributes(new Attributes.Attribute("constraint","등록된 이미지가 없으면 null")),
            fieldWithPath("data.diaryList[].diaryInfo[].dateTime").type(JsonFieldType.STRING).description("일기 작성 날짜 및 시간 : YYYY-MM-DDTHH:MM:SS")
    );

    List<FieldDescriptor> resultDescriptorsForDiary = List.of(
            fieldWithPath("status").type(JsonFieldType.STRING).description("응답 상태"),
            fieldWithPath("code").type(JsonFieldType.STRING).description("상태 코드"),
            fieldWithPath("data").type(JsonFieldType.OBJECT).optional().description("data"),
            fieldWithPath("data.diaryId").type(JsonFieldType.NUMBER).description("새롭게 생성된 다이어리 고유 id"),
            fieldWithPath("data.greenroomId").type(JsonFieldType.NUMBER).description("그린룸 id"),
            fieldWithPath("data.greenroomName").type(JsonFieldType.STRING).description("그린룸 별명"),
            fieldWithPath("data.title").type(JsonFieldType.STRING).description("일기 제목"),
            fieldWithPath("data.content").type(JsonFieldType.STRING).description("일기 본문"),
            fieldWithPath("data.imageUrl").type(JsonFieldType.STRING).description("일기 이미지").optional().attributes(new Attributes.Attribute("constraint","등록된 이미지가 없으면 null")),
            fieldWithPath("data.dateTime").type(JsonFieldType.STRING).description("일기 작성 날짜 및 시간 : YYYY-MM-DDTHH:MM:SS")
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
                        .patch("/api/greenroom/{greenroom_id}/items",greenroomId)
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

    @Test
    @Transactional
    public void 그린룸_삭제() throws Exception{
        //given
        User user = signupForTest();
        GreenRoom greenRoom = createGreenRoom(user);

        //when
        String token = getTokenForTest((long) (10*1000));
        ResultActions resultActions =  mockMvc.perform( // api 실행
                RestDocumentationRequestBuilders
                        .delete("/api/greenroom/{greenroom_id}",greenRoom.getGreenroomId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer "+token));

        //then
        resultActions.andExpect(status().is(HttpStatus.NO_CONTENT.value()));

        //문서화
        resultActions.andDo(document("api/greenroom/delete/"+1,
                preprocessRequest(prettyPrint(),modifyUris().scheme("https").host("greenroom-server.site").removePort()),   // (2)
                preprocessResponse(prettyPrint(), getModifiedHeader()),  // (3)
                requestHeaders(headerWithName("Authorization").description("Bearer : 사용자 access Token")),
                pathParameters(pathParameterForGreenroomId),
                resource(
                        ResourceSnippetParameters.builder()
                                .tag("그린룸") // 문서에서 api들이 태그로 분류됨
                                .summary("그린룸 삭제 api") // api 이름
                                .description("특정 그린룸을 삭제함") // api 설명
                                .build()))

        );
    }

    private ResultActions getResultActionsForGreenroomMemo(Long greenroomId, MemoRequestDto memoRequestDto) throws Exception {

        String token = getTokenForTest((long) (10*1000));
        return mockMvc.perform( // api 실행
                RestDocumentationRequestBuilders
                        .patch("/api/greenroom/{greenroom_id}/memo",greenroomId)
                        .content(mapper.writeValueAsString(memoRequestDto))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer "+token));
    }

    private RestDocumentationResultHandler getDocumentForGreenroomMemo(Integer identifier){
        return document("api/greenroom/memo/"+identifier,
                preprocessRequest(prettyPrint(),modifyUris().scheme("https").host("greenroom-server.site").removePort()),   // (2)
                preprocessResponse(prettyPrint(), getModifiedHeader()),  // (3)
                requestHeaders(headerWithName("Authorization").description("Bearer : 사용자 access Token")),
                pathParameters(pathParameterForGreenroomId),
                requestFields(requestBodyDescriptorsForGreenroomMemo),
                resource(
                        ResourceSnippetParameters.builder()
                                .tag("그린룸") // 문서에서 api들이 태그로 분류됨
                                .summary("그린룸 메모 등록 api") // api 이름
                                .description("그린룸 메모를 등록/변경함.") // api 설명
                                .build()));
    }


    @Test
    @Transactional
    public void 그린룸_메모등록_성공() throws Exception {
        //given
        MemoRequestDto memoRequestDto = new MemoRequestDto("나 메모 아니다");
        User user = signupForTest();
        GreenRoom greenRoom = createGreenRoom(user);

        //when
        ResultActions resultActions = getResultActionsForGreenroomMemo(greenRoom.getGreenroomId(),memoRequestDto);

        //then
        resultActions.andExpect(status().is(HttpStatus.NO_CONTENT.value()));

        //문서화
        resultActions.andDo(getDocumentForGreenroomMemo(1));
    }

    @Test
    @Transactional
    public void 그린룸_메모등록_실패1() throws Exception {
        //given
        MemoRequestDto memoRequestDto = new MemoRequestDto("나 메모 아니다");

        //when
        ResultActions resultActions = getResultActionsForGreenroomMemo(100L,memoRequestDto);

        //then
        resultActions.andExpect(status().is(ResponseCodeEnum.GREENROOM_NOT_FOUND.getStatus().value())).andExpect(jsonPath("code").value(ResponseCodeEnum.GREENROOM_NOT_FOUND.getCode()));

        //문서화
        resultActions.andDo(getDocumentForGreenroomMemo(2));
    }

    @Test
    @Transactional
    public void 그린룸_메모등록_실패2() throws Exception {
        //given
        MemoRequestDto memoRequestDto = new MemoRequestDto("메모 아니다.메모 아니다.메모 아니다.메모 아니다.메모 아니다.메모 아니다.메모 아니다.메모 아니다.메모 아니다.");
        User user = signupForTest();
        GreenRoom greenRoom = createGreenRoom(user);

        //when
        ResultActions resultActions = getResultActionsForGreenroomMemo(greenRoom.getGreenroomId(),memoRequestDto);

        //then
        resultActions.andExpect(status().is(ResponseCodeEnum.TOO_LONG_STRING.getStatus().value())).andExpect(jsonPath("code").value(ResponseCodeEnum.TOO_LONG_STRING.getCode()));

        //문서화
        resultActions.andDo(getDocumentForGreenroomMemo(3));
    }

    private ResultActions getResultActionsForGreenroomPlant(Long greenroomId, GreenroomPlantRequestDto greenroomPlantRequestDto) throws Exception {
        String token = getTokenForTest((long) (10*1000));
        return mockMvc.perform( // api 실행
                RestDocumentationRequestBuilders
                        .patch("/api/greenroom/{greenroom_id}/plant",greenroomId)
                        .content(mapper.writeValueAsString(greenroomPlantRequestDto))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer "+token));
    }

    private RestDocumentationResultHandler getDocumentForGreenroomPlant(Integer identifier){
        return document("api/greenroom/plant/"+identifier,
                preprocessRequest(prettyPrint(),modifyUris().scheme("https").host("greenroom-server.site").removePort()),   // (2)
                preprocessResponse(prettyPrint(), getModifiedHeader()),  // (3)
                requestHeaders(headerWithName("Authorization").description("Bearer : 사용자 access Token")),
                pathParameters(pathParameterForGreenroomId),
                requestFields(requestBodyDescriptorsForGreenroomPlant),
                responseFields(resultDescriptorsForGreenroomDetails),
                resource(
                        ResourceSnippetParameters.builder()
                                .tag("그린룸") // 문서에서 api들이 태그로 분류됨
                                .summary("그린룸 메모 등록 api") // api 이름
                                .description("그린룸 메모를 등록/변경함.") // api 설명
                                .build()));
    }

    @Test
    @Transactional
    public void 그린룸_식물등록_성공() throws Exception {
        //given
        GreenroomPlantRequestDto greenroomPlantRequestDto = new GreenroomPlantRequestDto(1L);
        User user = signupForTest();
        GreenRoom greenRoom = createGreenRoom(user);
        greenRoom.updateCreationDate(LocalDateTime.now().minusDays(2));

        //when
        ResultActions resultActions = getResultActionsForGreenroomPlant(greenRoom.getGreenroomId(),greenroomPlantRequestDto);

        //then
        resultActions.andExpect(status().isOk());

        //문서화
        resultActions.andDo(getDocumentForGreenroomPlant(1));
    }

    @Test
    @Transactional
    public void 그린룸_식물등록_실패1() throws Exception {
        //given
        GreenroomPlantRequestDto greenroomPlantRequestDto = new GreenroomPlantRequestDto(1L);

        //when
        ResultActions resultActions = getResultActionsForGreenroomPlant(100L,greenroomPlantRequestDto);

        //then
        resultActions.andExpect(status().is(ResponseCodeEnum.GREENROOM_NOT_FOUND.getStatus().value())).andExpect(jsonPath("code").value(ResponseCodeEnum.GREENROOM_NOT_FOUND.getCode()));

        //문서화
        resultActions.andDo(getDocumentForGreenroomPlant(2));
    }

    @Test
    @Transactional
    public void 그린룸_식물등록_실패2() throws Exception {
        //given
        GreenroomPlantRequestDto greenroomPlantRequestDto = new GreenroomPlantRequestDto(1000L);
        User user = signupForTest();
        GreenRoom greenRoom = createGreenRoom(user);

        //when
        ResultActions resultActions = getResultActionsForGreenroomPlant(greenRoom.getGreenroomId(),greenroomPlantRequestDto);

        //then
        resultActions.andExpect(status().is(ResponseCodeEnum.PLANT_NOT_FOUND.getStatus().value())).andExpect(jsonPath("code").value(ResponseCodeEnum.PLANT_NOT_FOUND.getCode()));

        //문서화
        resultActions.andDo(getDocumentForGreenroomPlant(3));
    }


    private ResultActions getResultActionsForGreenroomInfoUpdate(Long greenroomId, MockMultipartFile image,MockMultipartFile data) throws Exception {

        String token = getTokenForTest((long) (10*1000));
        return mockMvc.perform( // api 실행
                RestDocumentationRequestBuilders
                        .multipart("/api/greenroom/{greenroom_id}/info",greenroomId)
                        .file(image)
                        .file(data)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer "+token)
                        .with(request -> {
                            request.setMethod("PATCH"); // PATCH로 변경!
                            return request;}));
    }

    private RestDocumentationResultHandler getDocumentForGreenroomInfoUpdate(Integer identifier){
        return document("api/greenroom/info/update/"+identifier,
                preprocessRequest(prettyPrint(),modifyUris().scheme("https").host("greenroom-server.site").removePort()),
                preprocessResponse(prettyPrint(), getModifiedHeader()),
                pathParameters(pathParameterForGreenroomId),
                requestParts(requestPartDescriptorsForGreenroomInfoUpdate),
                requestPartFields("data",requestPartFieldDescriptorsForGreenroomInfoUpdate),
                responseFields(resultDescriptorsForGreenroomDetails), // responseBody 설명
                requestHeaders(headerWithName("Authorization").description("Bearer : 사용자 access Token")),
                resource(ResourceSnippetParameters.builder()
                                .tag("그린룸") // 문서에서 api들이 태그로 분류됨
                                .summary("그린룸 식물 정보 편집 api") // api 이름
                                .description("그린룸 정보를 편집함.") // api 설명
                                .build()));
    }

    @Test
    @Transactional
    public void 그린룸_식물정보_편집_성공() throws Exception {
        //given
        User user = signupForTest();
        GreenRoom greenRoom = createGreenRoom(user);
        greenRoom.updateCreationDate(LocalDateTime.now().minusDays(2));

        GreenroomInfoUpdateRequestDto greenroomInfoUpdateRequestDto = new GreenroomInfoUpdateRequestDto("해롱해롱이",true, GreenroomInfoUpdateRequestDto.ImageAction.UPLOAD);
        MockMultipartFile image = getTestMultiPartFile();
        MockMultipartFile data = new MockMultipartFile("data", "", "application/json", mapper.writeValueAsString(greenroomInfoUpdateRequestDto).getBytes());

        //when
        ResultActions resultActions = getResultActionsForGreenroomInfoUpdate(greenRoom.getGreenroomId(),image,data);

        //then
        resultActions.andExpect(status().isOk());

        //문서화
        resultActions.andDo(getDocumentForGreenroomInfoUpdate(1));
    }

    @Test
    @Transactional
    public void 그린룸_식물정보_편집_실패1() throws Exception {
        //given
        GreenroomInfoUpdateRequestDto greenroomInfoUpdateRequestDto = new GreenroomInfoUpdateRequestDto("해롱해롱이",true, GreenroomInfoUpdateRequestDto.ImageAction.UPLOAD);
        MockMultipartFile image = getTestMultiPartFile();
        MockMultipartFile data = new MockMultipartFile("data", "", "application/json", mapper.writeValueAsString(greenroomInfoUpdateRequestDto).getBytes());

        //when
        ResultActions resultActions = getResultActionsForGreenroomInfoUpdate(100L,image,data);

        //then
        resultActions.andExpect(status().is(ResponseCodeEnum.GREENROOM_NOT_FOUND.getStatus().value())).andExpect(jsonPath("code").value(ResponseCodeEnum.GREENROOM_NOT_FOUND.getCode()));

        //문서화
        resultActions.andDo(getDocumentForGreenroomInfoUpdate(2));
    }

    @Test
    @Transactional
    public void 그린룸_식물정보_편집_실패2() throws Exception {
        //given
        User user = signupForTest();
        GreenRoom greenRoom = createGreenRoom(user);
        greenRoom.updateCreationDate(LocalDateTime.now().minusDays(2));

        GreenroomInfoUpdateRequestDto greenroomInfoUpdateRequestDto = new GreenroomInfoUpdateRequestDto("해롱해롱이",true, GreenroomInfoUpdateRequestDto.ImageAction.UPLOAD);
        MockMultipartFile image = getInvalidTestMultiPartFile();
        MockMultipartFile data = new MockMultipartFile("data", "", "application/json", mapper.writeValueAsString(greenroomInfoUpdateRequestDto).getBytes());


        //when
        ResultActions resultActions = getResultActionsForGreenroomInfoUpdate(greenRoom.getGreenroomId(),image,data);

        //then
        resultActions.andExpect(status().is(ResponseCodeEnum.INVALID_IMAGE_FORMAT.getStatus().value())).andExpect(jsonPath("code").value(ResponseCodeEnum.INVALID_IMAGE_FORMAT.getCode()));

        //문서화
        resultActions.andDo(getDocumentForGreenroomInfoUpdate(3));
    }

    @Test
    @Transactional
    public void 그린룸_식물정보_편집_실패3() throws Exception {
        //given
        User user = signupForTest();
        GreenRoom greenRoom = createGreenRoom(user);
        greenRoom.updateCreationDate(LocalDateTime.now().minusDays(2));

        GreenroomInfoUpdateRequestDto greenroomInfoUpdateRequestDto = new GreenroomInfoUpdateRequestDto("해롱해롱이",true, GreenroomInfoUpdateRequestDto.ImageAction.UPLOAD);
        MockMultipartFile image = getInvalidTestMultiPartFile();
        MockMultipartFile data = new MockMultipartFile("data", "", "application/json", mapper.writeValueAsString(greenroomInfoUpdateRequestDto).getBytes());

        //when
        doThrow(new CustomException(ResponseCodeEnum.FAIL_TO_UPLOAD_IMAGE)).when(mockitoGreenroomService).updateGreenroomInfo(greenRoom.getGreenroomId(),greenroomInfoUpdateRequestDto,image);
        ResultActions resultActions = getResultActionsForGreenroomInfoUpdate(greenRoom.getGreenroomId(),image,data);

        //then
        resultActions.andExpect(status().is(ResponseCodeEnum.FAIL_TO_UPLOAD_IMAGE.getStatus().value())).andExpect(jsonPath("code").value(ResponseCodeEnum.FAIL_TO_UPLOAD_IMAGE.getCode()));

        //문서화
        resultActions.andDo(getDocumentForGreenroomInfoUpdate(4));
    }

    private ResultActions getResultActionsForGetActivityInfo(Long greenroomId) throws Exception {

        String token = getTokenForTest((long) (10*1000));
        return mockMvc.perform( // api 실행
                RestDocumentationRequestBuilders
                        .get("/api/greenroom/{greenroom_id}/activity",greenroomId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer "+token));
    }

    private RestDocumentationResultHandler getDocumentForGetActivityInfo(Integer identifier){
        return document("api/greenroom/activity/get/"+identifier,
                preprocessRequest(prettyPrint(),modifyUris().scheme("https").host("greenroom-server.site").removePort()),
                preprocessResponse(prettyPrint(), getModifiedHeader()),
                pathParameters(pathParameterForGreenroomId),
                responseFields(resultDescriptorsForGetActivityInfo), // responseBody 설명
                requestHeaders(headerWithName("Authorization").description("Bearer : 사용자 access Token")),
                resource(ResourceSnippetParameters.builder()
                        .tag("그린룸") // 문서에서 api들이 태그로 분류됨
                        .summary("그린룸 주기 활성화/비활성화 정보 조회 api") // api 이름
                        .description("그린룸 주기 활성화/비활성화 정보를 조회함. 활성화된 주기의 경우 상세 정보를 반환함.") // api 설명
                        .build()));
    }


    @Test
    @Transactional
    public void 그린룸_주기정보_조회_성공() throws Exception {
        //given
        User user = signupForTest();
        GreenRoom greenRoom = createGreenRoom(user);
        greenRoom.updateCreationDate(LocalDateTime.now().minusDays(2));

        //when
        ResultActions resultActions = getResultActionsForGetActivityInfo(greenRoom.getGreenroomId());

        //then
        resultActions.andExpect(status().isOk());

        //문서화
        resultActions.andDo(getDocumentForGetActivityInfo(1));
    }

    @Test
    @Transactional
    public void 그린룸_주기정보_조회_실패() throws Exception {
        //given

        //when
        ResultActions resultActions = getResultActionsForGetActivityInfo(100L);

        //then
        resultActions.andExpect(status().is(ResponseCodeEnum.GREENROOM_NOT_FOUND.getStatus().value())).andExpect(jsonPath("code").value(ResponseCodeEnum.GREENROOM_NOT_FOUND.getCode()));

        //문서화
        resultActions.andDo(getDocumentForGetActivityInfo(2));
    }

    private ResultActions getResultActionsForUpdateActivityStatus(Long greenroomId, ActivityStatusUpdateRequestDto activityStatusUpdateRequestDto) throws Exception {

        String token = getTokenForTest((long) (10*1000));
        return mockMvc.perform( // api 실행
                RestDocumentationRequestBuilders
                        .patch("/api/greenroom/{greenroom_id}/activity/status",greenroomId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(activityStatusUpdateRequestDto))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer "+token));
    }

    private RestDocumentationResultHandler getDocumentForUpdateActivityStatus(Integer identifier){
        return document("api/greenroom/activity/status/"+identifier,
                preprocessRequest(prettyPrint(),modifyUris().scheme("https").host("greenroom-server.site").removePort()),
                preprocessResponse(prettyPrint(), getModifiedHeader()),
                pathParameters(pathParameterForGreenroomId),
                responseFields(resultDescriptorsForGetActivityInfo), // responseBody 설명
                requestHeaders(headerWithName("Authorization").description("Bearer : 사용자 access Token")),
                requestFields(requestBodyDescriptorsForUpdateActivityStatus),
                resource(ResourceSnippetParameters.builder()
                        .tag("그린룸") // 문서에서 api들이 태그로 분류됨
                        .summary("그린룸 주기 활성화/비활성화 변경 api") // api 이름
                        .description("그린룸 주기를 활성화/비활성화함.") // api 설명
                        .build()));
    }


    @Test
    @Transactional
    public void 그린룸_주기_상태_변경_성공() throws Exception {
        //given
        User user = signupForTest();
        GreenRoom greenRoom = createGreenRoom(user);
        greenRoom.updateCreationDate(LocalDateTime.now().minusDays(2));
        ActivityStatusUpdateRequestDto activityStatusUpdateRequestDto = new ActivityStatusUpdateRequestDto(List.of(1L,2L), List.of(3L));

        //when
        ResultActions resultActions = getResultActionsForUpdateActivityStatus(greenRoom.getGreenroomId(),activityStatusUpdateRequestDto);

        //then
        resultActions.andExpect(status().isOk());

        //문서화
        resultActions.andDo(getDocumentForUpdateActivityStatus(1));
    }

    @Test
    @Transactional
    public void 그린룸_주기_상태_변경_실패() throws Exception {
        //given
        ActivityStatusUpdateRequestDto activityStatusUpdateRequestDto = new ActivityStatusUpdateRequestDto(List.of(1L,2L), List.of(3L,4L,5L,6L));

        //when
        ResultActions resultActions = getResultActionsForUpdateActivityStatus(100L,activityStatusUpdateRequestDto);

        //then
        resultActions.andExpect(status().is(ResponseCodeEnum.GREENROOM_NOT_FOUND.getStatus().value())).andExpect(jsonPath("code").value(ResponseCodeEnum.GREENROOM_NOT_FOUND.getCode()));

        //문서화
        resultActions.andDo(getDocumentForUpdateActivityStatus(2));
    }


    private ResultActions getResultActionsForUpdateActivityInfo(Long greenroomId, ActivityInfoUpdateRequestDto activityInfoUpdateRequestDto) throws Exception {

        String token = getTokenForTest((long) (10*1000));
        return mockMvc.perform( // api 실행
                RestDocumentationRequestBuilders
                        .patch("/api/greenroom/{greenroom_id}/activity",greenroomId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(activityInfoUpdateRequestDto))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer "+token));
    }

    private RestDocumentationResultHandler getDocumentForUpdateActivityInfo(Integer identifier){
        return document("api/greenroom/activity/patch/"+identifier,
                preprocessRequest(prettyPrint(),modifyUris().scheme("https").host("greenroom-server.site").removePort()),
                preprocessResponse(prettyPrint(), getModifiedHeader()),
                pathParameters(pathParameterForGreenroomId),
                responseFields(resultDescriptorsForGetActivityInfo), // responseBody 설명
                requestHeaders(headerWithName("Authorization").description("Bearer : 사용자 access Token")),
                requestFields(requestBodyDescriptorsForUpdateActivityInfo),
                resource(ResourceSnippetParameters.builder()
                        .tag("그린룸") // 문서에서 api들이 태그로 분류됨
                        .summary("그린룸 주기 정보 변경 api") // api 이름
                        .description("그린룸 주기 정보를 변경함.") // api 설명
                        .build()));
    }

    @Test
    @Transactional
    public void 그린룸_주기_변경_성공() throws Exception {
        //given
        User user = signupForTest();
        GreenRoom greenRoom = createGreenRoom(user);
        greenRoom.updateCreationDate(LocalDateTime.now().minusDays(2));
        ActivityInfoUpdateRequestDto activityInfoUpdateRequestDto = new ActivityInfoUpdateRequestDto(
            List.of(new ActivityInfoUpdateRequestDto.ActivityInfoUpdateDto(1L,"2025-03-10",10)));

        //when
        ResultActions resultActions = getResultActionsForUpdateActivityInfo(greenRoom.getGreenroomId(),activityInfoUpdateRequestDto);

        //then
        resultActions.andExpect(status().isOk());

        //문서화
        resultActions.andDo(getDocumentForUpdateActivityInfo(1));
    }

    @Test
    @Transactional
    public void 그린룸_주기_변경_실패() throws Exception {
        //given
        ActivityInfoUpdateRequestDto activityInfoUpdateRequestDto = new ActivityInfoUpdateRequestDto(
                List.of(new ActivityInfoUpdateRequestDto.ActivityInfoUpdateDto(1L,"2025-03-10",10)));

        //when
        ResultActions resultActions = getResultActionsForUpdateActivityInfo(100L,activityInfoUpdateRequestDto);

        //then
        resultActions.andExpect(status().is(ResponseCodeEnum.GREENROOM_NOT_FOUND.getStatus().value())).andExpect(jsonPath("code").value(ResponseCodeEnum.GREENROOM_NOT_FOUND.getCode()));

        //문서화
        resultActions.andDo(getDocumentForUpdateActivityInfo(2));
    }


    private ResultActions getResultActionsForCalenderInfo(String date) throws Exception {
        String token = getTokenForTest((long) (10*1000));
        return mockMvc.perform( // api 실행
                RestDocumentationRequestBuilders
                        .get("/api/greenroom/calendar")
                        .param("date",date)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer "+token));
    }

    private RestDocumentationResultHandler getDocumentForCalenderInfo(Integer identifier){
        return document("api/greenroom/calendar/"+identifier,
                preprocessRequest(prettyPrint(),modifyUris().scheme("https").host("greenroom-server.site").removePort()),
                preprocessResponse(prettyPrint(), getModifiedHeader()),
                queryParameters(parameterDescriptorsForCalenderInfo),
                responseFields(resultDescriptorsForCalenderInfo), // responseBody 설명
                requestHeaders(headerWithName("Authorization").description("Bearer : 사용자 access Token")),
                resource(ResourceSnippetParameters.builder()
                        .tag("그린룸") // 문서에서 api들이 태그로 분류됨
                        .summary("그린룸 날짜별 정보 api") // api 이름
                        .description("그린룸 달력 정보 api 조회") // api 설명
                        .build()));
    }
    private final List<ParameterDescriptor> parameterDescriptorsForCalenderInfo = List.of(
            parameterWithName("date").description("조회 날짜").attributes(new Attributes.Attribute("constraint","yyyy-mm-dd")),
            parameterWithName("type").description("특정 type의 주기를 조회하는 경우 activity id 전달").optional().attributes(new Attributes.Attribute("constraint","상단 표에 작성된 activity id"),new Attributes.Attribute("default","모든 activity의 활동 내역을 조회"))
    );

    @Test
    @Transactional
    public void 그린룸_달력정보_조회_성공() throws Exception {
        //given
        User user = signupForTest();
        createGreenRoomForCalender(user);

        //when
        ResultActions resultActions = getResultActionsForCalenderInfo(LocalDate.now().toString());

        //then
        resultActions.andExpect(status().isOk());

        //문서화
        resultActions.andDo(getDocumentForCalenderInfo(1));
    }

    private ResultActions getResultActionsForDiaryCreation(MockMultipartFile image,MockMultipartFile data) throws Exception {

        String token = getTokenForTest((long) (10*1000));

        return mockMvc.perform( // api 실행
                RestDocumentationRequestBuilders
                        .multipart("/api/greenroom/diary")
                        .file(image)
                        .file(data)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer "+token));
    }

    private RestDocumentationResultHandler getDocumentForDiaryCreation(Integer identifier){
        return document("api/greenroom/diary/post/"+identifier,
                preprocessRequest(prettyPrint(),modifyUris().scheme("https").host("greenroom-server.site").removePort()),   // (2)
                preprocessResponse(prettyPrint(), getModifiedHeader()),  // (3)
                requestParts(requestPartDescriptorsForDiaryCreation),
                requestPartFields("data",requestPartFieldDescriptorsForDiaryCreation),
                responseFields(resultDescriptorsForDiaryCreation), // responseBody 설명
                requestHeaders(headerWithName("Authorization").description("Bearer : 사용자 access Token")),
                resource(
                        ResourceSnippetParameters.builder()
                                .tag("그린룸") // 문서에서 api들이 태그로 분류됨
                                .summary("일기 작성 api") // api 이름
                                .description("그린룸 일기 작성") // api 설명
                                .build()));
    }

    @Test
    @Transactional
    public void 일기_작성_성공() throws Exception {
        //given

        //test용 data 생성

        User user =signupForTest();
        createGreenRoom(user);

        DiaryCreationRequestDto request = new DiaryCreationRequestDto("일기 제목입니다!!","일기 본문입니다!!","2025-04-05");
        MockMultipartFile image = getTestMultiPartFile();
        MockMultipartFile data = new MockMultipartFile("data", "", "application/json", mapper.writeValueAsString(request).getBytes());

        //when
        ResultActions resultActions = getResultActionsForDiaryCreation(image,data);

        //then
        resultActions.andExpect(status().isCreated());

        //문서화
        resultActions.andDo(getDocumentForDiaryCreation(1));

    }

    @Test
    @Transactional
    public void 일기_작성_실패1() throws Exception {
        //given

        //test용 data 생성
        User user =signupForTest();

        DiaryCreationRequestDto request = new DiaryCreationRequestDto("일기 제목입니다!!","일기 본문입니다!!","2025-04-05");
        MockMultipartFile image = getTestMultiPartFile();
        MockMultipartFile data = new MockMultipartFile("data", "", "application/json", mapper.writeValueAsString(request).getBytes());

        //when
        ResultActions resultActions = getResultActionsForDiaryCreation(image,data);

        //then
        resultActions.andExpect(status().is(ResponseCodeEnum.GREENROOM_NOT_FOUND.getStatus().value())).andExpect(jsonPath("code").value(ResponseCodeEnum.GREENROOM_NOT_FOUND.getCode()));

        //문서화
        resultActions.andDo(getDocumentForDiaryCreation(2));
    }

    @Test
    @Transactional
    public void 일기_작성_실패2() throws Exception {
        //given

        //test 용 data
        User user = signupForTest();
        createGreenRoom(user);

        DiaryCreationRequestDto request = new DiaryCreationRequestDto("일기 제목입니다!!","일기 본문입니다!!","2025-04-05");
        MockMultipartFile image = getInvalidTestMultiPartFile();
        MockMultipartFile data = new MockMultipartFile("data", "", "application/json", mapper.writeValueAsString(request).getBytes());

        //when
        ResultActions resultActions = getResultActionsForDiaryCreation(image,data);

        //then
        resultActions.andExpect(status().is(ResponseCodeEnum.INVALID_IMAGE_FORMAT.getStatus().value())).andExpect(jsonPath("code").value(ResponseCodeEnum.INVALID_IMAGE_FORMAT.getCode()));

        //문서화
        resultActions.andDo(getDocumentForDiaryCreation(3));
    }

    @Test
    @Transactional
    public void 일기_작성_실패3() throws Exception {
        //given
        //test 용 data
        User user =signupForTest();

        DiaryCreationRequestDto request = new DiaryCreationRequestDto("일기 제목임~!!","일기 본문입니다!!","2025-04-05");
        MockMultipartFile image = getTestMultiPartFile();
        MockMultipartFile data = new MockMultipartFile("data", "", "application/json", mapper.writeValueAsString(request).getBytes());

        //when
        doThrow(new CustomException(ResponseCodeEnum.FAIL_TO_UPLOAD_IMAGE)).when(mockitoGreenroomService).createDiary(EMAIL,request,image);
        ResultActions resultActions = getResultActionsForDiaryCreation(image,data);

        //then
        resultActions.andExpect(status().is(ResponseCodeEnum.FAIL_TO_UPLOAD_IMAGE.getStatus().value())).andExpect(jsonPath("code").value(ResponseCodeEnum.FAIL_TO_UPLOAD_IMAGE.getCode()));

        //문서화
        resultActions.andDo(getDocumentForDiaryCreation(4));
    }


    private ResultActions getResultActionsForDiaryList(String date) throws Exception {

        String token = getTokenForTest((long) (10*1000));

        return mockMvc.perform( // api 실행
                RestDocumentationRequestBuilders
                        .get("/api/greenroom/diaries")
                        .param("date",date)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer "+token));
    }

    private RestDocumentationResultHandler getDocumentForDiaryList(Integer identifier){
        return document("api/greenroom/diaries/"+identifier,
                preprocessRequest(prettyPrint(),modifyUris().scheme("https").host("greenroom-server.site").removePort()),   // (2)
                preprocessResponse(prettyPrint(), getModifiedHeader()),  // (3)
                queryParameters(queryParametersForDiaryList),
                responseFields(resultDescriptorsForDiaryList), // responseBody 설명
                requestHeaders(headerWithName("Authorization").description("Bearer : 사용자 access Token")),
                resource(
                        ResourceSnippetParameters.builder()
                                .tag("그린룸") // 문서에서 api들이 태그로 분류됨
                                .summary("연도/월 별 작성된 일기 조회 api") // api 이름
                                .description("특정 연도-월에 작성된 모든 일기 목록을 조회함.") // api 설명
                                .build()));
    }

    @Test
    @Transactional
    public void 일기목록_조회_성공() throws Exception {
        //given

        //test용 data 생성

        User user =signupForTest();
        GreenRoom greenRoom = createGreenRoom(user);
        GreenRoom greenRoom1 = createGreenRoom2(user);


        Diary diary1 = Diary.createDiary("일기제목111","일기 본문111",greenRoom,null,LocalDate.of(2025,4,5)); diary1.updateCreateDate(LocalDateTime.now());
        Diary diary2 = Diary.createDiary("일기제목222","일기 본문222",greenRoom,null,LocalDate.of(2025,4,3));diary2.updateCreateDate(LocalDateTime.now());
        Diary diary3 = Diary.createDiary("일기제목333","일기 본문333",greenRoom1,null,LocalDate.of(2025,4,2));diary3.updateCreateDate(LocalDateTime.now());
        Diary diary4 = Diary.createDiary("일기제목444","일기 본문444",greenRoom1,null,LocalDate.of(2025,4,3));diary4.updateCreateDate(LocalDateTime.now());

        diaryRepository.save(diary1);diaryRepository.save(diary2);diaryRepository.save(diary3);diaryRepository.save(diary4);

        //when
        ResultActions resultActions = getResultActionsForDiaryList("2025-04");

        //then
        resultActions.andExpect(status().isOk());

        //문서화
        resultActions.andDo(getDocumentForDiaryList(1));

    }


    private ResultActions getResultActionsForDiary(Long diaryId) throws Exception {

        String token = getTokenForTest((long) (10*1000));
        return mockMvc.perform( // api 실행
                RestDocumentationRequestBuilders
                        .get("/api/greenroom/diaries/{diary_id}",diaryId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer "+token));
    }

    private RestDocumentationResultHandler getDocumentForDiary(Integer identifier){
        return document("api/greenroom/diaries/specific/"+identifier,
                preprocessRequest(prettyPrint(),modifyUris().scheme("https").host("greenroom-server.site").removePort()),
                preprocessResponse(prettyPrint(), getModifiedHeader()),
                pathParameters(pathParameterForDiary),
                responseFields(resultDescriptorsForDiary), // responseBody 설명
                requestHeaders(headerWithName("Authorization").description("Bearer : 사용자 access Token")),
                resource(ResourceSnippetParameters.builder()
                        .tag("그린룸") // 문서에서 api들이 태그로 분류됨
                        .summary("일기 상세 조회 api") // api 이름
                        .description("특정 일기를 상세 조회함.") // api 설명
                        .build()));
    }

    @Test
    @Transactional
    public void 일기_조회_성공() throws Exception {
        //given

        //test용 data 생성
        User user =signupForTest();
        GreenRoom greenRoom = createGreenRoom(user);

        Diary diary = Diary.createDiary("일기제목111","일기 본문111",greenRoom,null,LocalDate.of(2025,4,5));
        diary.updateCreateDate(LocalDateTime.now());
        diaryRepository.save(diary);
        //when
        ResultActions resultActions = getResultActionsForDiary(diary.getDiaryId());

        //then
        resultActions.andExpect(status().isOk());

        //문서화
        resultActions.andDo(getDocumentForDiary(1));

    }

    @Test
    @Transactional
    public void 일기_조회_실패1() throws Exception {
        //given

        //test용 data 생성
        User user =signupForTest();
        GreenRoom greenRoom = createGreenRoom(user);

        //when
        ResultActions resultActions = getResultActionsForDiary(10L);

        //then
        resultActions.andExpect(status().is(ResponseCodeEnum.DIARY_NOT_FOUND.getStatus().value())).andExpect(jsonPath("code").value(ResponseCodeEnum.DIARY_NOT_FOUND.getCode()));

        //문서화
        resultActions.andDo(getDocumentForDiary(2));

    }

    @Test
    @Transactional
    public void 일기_조회_실패2() throws Exception {
        //given

        //test용 data 생성
        User user =signupForTest2();
        GreenRoom greenRoom = createGreenRoom(user);
        Diary diary = Diary.createDiary("일기제목111","일기 본문111",greenRoom,null,LocalDate.of(2025,4,5));
        diary.updateCreateDate(LocalDateTime.now());
        diaryRepository.save(diary);

        //when
        ResultActions resultActions = getResultActionsForDiary(diary.getDiaryId());

        //then
        resultActions.andExpect(status().is(ResponseCodeEnum.NOT_AUTHORIZATION.getStatus().value())).andExpect(jsonPath("code").value(ResponseCodeEnum.NOT_AUTHORIZATION.getCode()));

        //문서화
        resultActions.andDo(getDocumentForDiary(3));

    }

}
