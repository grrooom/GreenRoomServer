package com.greenroom.server.api;

import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.greenroom.server.api.config.TestExecutionListener;
import com.greenroom.server.api.domain.greenroom.entity.Adornment;
import com.greenroom.server.api.domain.greenroom.entity.GreenRoom;
import com.greenroom.server.api.domain.greenroom.entity.Plant;
import com.greenroom.server.api.domain.greenroom.entity.Todo;
import com.greenroom.server.api.domain.greenroom.repository.*;
import com.greenroom.server.api.domain.user.entity.User;
import com.greenroom.server.api.domain.user.repository.GradeRepository;
import com.greenroom.server.api.domain.user.repository.UserRepository;
import com.greenroom.server.api.security.dto.SignupRequestDto;
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
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import org.springframework.restdocs.operation.preprocess.HeadersModifyingOperationPreprocessor;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;
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

import java.time.LocalDateTime;
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

        Plant plant = Plant.builder().commonName("해바라기").build();
        plantRepository.save(plant);

        GreenRoom greenRoom = new GreenRoom("test 그린룸",null,user,plant);
        greenRoomRepository.save(greenRoom);

        Todo todo = Todo.builder().useYn(true).greenRoom(greenRoom).activity(activityRepository.findAll().get(0)).nextTodoDate(LocalDateTime.now()).build();
        todoRepository.save(todo);

        adornmentRepository.save(new Adornment(itemRepository.findAll().get(0),greenRoom));


        return greenRoom;
    }

    List<FieldDescriptor> resultDescriptorsForGetGreenroom = List.of(
            fieldWithPath("status").type(JsonFieldType.STRING).description("응답 상태"),
            fieldWithPath("code").type(JsonFieldType.STRING).description("상태 코드"),
            fieldWithPath("data").type(JsonFieldType.OBJECT).optional().description("data").attributes(new Attributes.Attribute("constraint","등록된 식물이 없으면 null")),
            fieldWithPath("data.basicInfo").type(JsonFieldType.OBJECT).description("그린룸 기본 정보"),
            fieldWithPath("data.basicInfo.greenroomId").type(JsonFieldType.NUMBER).description("그린룸 id"),
            fieldWithPath("data.basicInfo.plantNickname").type(JsonFieldType.STRING).description("그린룸 별명"),
            fieldWithPath("data.basicInfo.plantName").type(JsonFieldType.STRING).description("식물 이름"),
            fieldWithPath("data.basicInfo.imageUrl").type(JsonFieldType.STRING).description("사용자가 등록한 식물 이미지.").optional().attributes(new Attributes.Attribute("constraint","등록된 사진이 없으면 null")),
            fieldWithPath("data.basicInfo.memo").type(JsonFieldType.STRING).description("사용자가 등록한 식물 메모").optional().attributes(new Attributes.Attribute("constraint","등록된 메모가 없으면 null")),
            fieldWithPath("data.todo").type(JsonFieldType.OBJECT).description("그린룸 할 일 정보"),
            fieldWithPath("data.todo.todoList").type(JsonFieldType.ARRAY).description("할 일 목록.").attributes(new Attributes.Attribute("constraint","목록이 없으면 빈 배열 반환")),
            fieldWithPath("data.todo.todoList[].activityId").type(JsonFieldType.NUMBER).description("할 일 id").optional(),
            fieldWithPath("data.todo.todoList[].activityName").type(JsonFieldType.STRING).description("할 일 이름").optional(),
            fieldWithPath("data.todo.todoList[].description").type(JsonFieldType.STRING).description("한국어 설명").optional(),
            fieldWithPath("data.todo.numberOfTodo").type(JsonFieldType.NUMBER).description("할 일 총 개수"),
            fieldWithPath("data.customItems").type(JsonFieldType.OBJECT).description("사용자가 등록한 그린룸 custom item"),
            fieldWithPath("data.customItems.hair_accessory").type(JsonFieldType.OBJECT).description("헤어핀 악세서리").attributes(new Attributes.Attribute("constraint","등록된 item이 없으면 null")).optional(),
            fieldWithPath("data.customItems.hair_accessory.itemId").type(JsonFieldType.NUMBER).description("헤어핀 악세서리 item id").optional(),
            fieldWithPath("data.customItems.hair_accessory.itemName").type(JsonFieldType.STRING).description("헤어핀 악세서리 item 이름").optional(),
            fieldWithPath("data.customItems.shape").type(JsonFieldType.OBJECT).description("식물 형태").attributes(new Attributes.Attribute("constraint","등록된 item이 없으면 null")).optional(),
            fieldWithPath("data.customItems.shape.itemId").type(JsonFieldType.NUMBER).description("식물 형태 item id").optional(),
            fieldWithPath("data.customItems.shape.itemName").type(JsonFieldType.STRING).description("식물 형태 item 이름").optional(),
            fieldWithPath("data.customItems.eyewear").type(JsonFieldType.OBJECT).description("안경 악세서리").attributes(new Attributes.Attribute("constraint","등록된 item이 없으면 null")).optional(),
            fieldWithPath("data.customItems.eyewear.itemId").type(JsonFieldType.NUMBER).description("안경 악세서리 item id").optional(),
            fieldWithPath("data.customItems.eyewear.itemName").type(JsonFieldType.STRING).description("안경 악세서리 item 이름").optional(),
            fieldWithPath("data.customItems.background_shelf").type(JsonFieldType.OBJECT).description("선반 배경 악세서리").attributes(new Attributes.Attribute("constraint","등록된 item이 없으면 null")).optional(),
            fieldWithPath("data.customItems.background_shelf.itemId").type(JsonFieldType.NUMBER).description("선반 배경 악세서리 item id").optional(),
            fieldWithPath("data.customItems.background_shelf.itemName").type(JsonFieldType.STRING).description("선반 배경 악세서리 item 이름").optional(),
            fieldWithPath("data.customItems.background_window").type(JsonFieldType.OBJECT).description("창문 배경 악세서리").attributes(new Attributes.Attribute("constraint","등록된 item이 없으면 null")).optional(),
            fieldWithPath("data.customItems.background_window.itemId").type(JsonFieldType.NUMBER).description("창문 배경 악세서리 item id").optional(),
            fieldWithPath("data.customItems.background_window.itemName").type(JsonFieldType.STRING).description("창문 배경 악세서리 item 이름").optional()
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
        User user = signupForTest();

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

}
