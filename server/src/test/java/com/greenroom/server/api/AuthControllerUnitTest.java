//package com.greenroom.server.api;
//import com.epages.restdocs.apispec.ResourceSnippetParameters;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.greenroom.server.api.security.controller.AuthController;
//import com.greenroom.server.api.security.dto.SignupRequestDto;
//import com.greenroom.server.api.security.service.CustomUserDetailService;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
//import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
//import org.springframework.http.MediaType;
//import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
//import org.springframework.restdocs.payload.FieldDescriptor;
//import org.springframework.security.test.context.support.WithMockUser;
//import org.springframework.test.context.bean.override.mockito.MockitoBean;
//import org.springframework.test.web.servlet.MockMvc;
//import org.springframework.test.web.servlet.ResultActions;
//
//import java.util.Arrays;
//import java.util.List;
//import java.util.stream.Collectors;
//
//import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
//import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.doNothing;
//import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
//import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//
//@WithMockUser
//@AutoConfigureRestDocs
//@WebMvcTest(controllers = AuthController.class)
//public class AuthControllerUnitTest {
//
//    @Autowired
//    private MockMvc mockMvc;
//
//    @MockitoBean
//    private CustomUserDetailService userDetailService;
//
//
//    private final ObjectMapper mapper = new ObjectMapper();
//
//    // enum 설명 적을 때 편리하게 적기 위한 메서드
//    private final <E extends Enum<E>> String getEnumValuesAsString(Class<E> enumClass) {
//        String enumValues = Arrays.stream(enumClass.getEnumConstants())
//                .map(Enum::name)
//                .collect(Collectors.joining(", "));
//        return " (종류: " + enumValues + ")";
//    }
//
//    //  기본 응답 관련해서 공통 descriptor로 처리
//    private final List<FieldDescriptor> resultDescriptors = List.of(
//            fieldWithPath("status").description("응답 상태")
//            ,fieldWithPath("code").description("상태 코드")
//            ,fieldWithPath("data").optional().description("data")
//    );
//
//
//    ////controller 단위 테스트 예제
//    @Test
//    @DisplayName("회원가입 api")
//    void 회원가입성공() throws Exception {
//
//        //given
//        SignupRequestDto signupRequest = new SignupRequestDto("test@gmail.com", "!123456");
//        doNothing().when(userDetailService).save(signupRequest);
//
//        // when
//        ResultActions resultActions = mockMvc.perform( // api 실행
//                RestDocumentationRequestBuilders
//                        .post("/api/auth/signup")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(mapper.writeValueAsString(signupRequest))
//                        .with(csrf())
//        );
//
//        // then
//        resultActions.andExpect(status().isCreated()); // 상태 코드 created인지 확인
//
//        resultActions.andDo( // 문서 작성
//                document(
//                        "회원가입-성공", // api의 id
//                        resource(
//                                ResourceSnippetParameters.builder()
//                                        .tag("😎 AUTH-인증/인가") // 문서에서 api들이 태그로 분류됨
//                                        .summary("회원가입 요청 api") // api 이름
//                                        .description("email과 password로 회원가입을 요청합니다. 인증된 email만 회원가입이 가능합니다.") // api 설명
//                                        .responseFields(resultDescriptors) // responseBody 설명
//                                        .build()
//                        )
//                )
//        );
//
//    }
//
//
//}
