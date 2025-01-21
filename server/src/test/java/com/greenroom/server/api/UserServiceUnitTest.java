package com.greenroom.server.api;

import com.greenroom.server.api.config.TestDatabaseExecutionListener;
import com.greenroom.server.api.domain.greenroom.entity.GreenRoom;
import com.greenroom.server.api.domain.greenroom.repository.GradeRepository;
import com.greenroom.server.api.domain.greenroom.repository.GreenRoomRepository;
import com.greenroom.server.api.domain.user.dto.UserExitRequestDto;
import com.greenroom.server.api.domain.user.entity.User;
import com.greenroom.server.api.domain.user.enums.UserExitReasonType;
import com.greenroom.server.api.domain.user.enums.UserStatus;
import com.greenroom.server.api.domain.user.repository.UserExitReasonRepository;
import com.greenroom.server.api.domain.user.repository.UserRepository;
import com.greenroom.server.api.domain.user.service.UserService;
import com.greenroom.server.api.security.dto.SignupRequestDto;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@SpringBootTest
@AutoConfigureMockMvc
@TestExecutionListeners(value = TestDatabaseExecutionListener.class, mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
@ActiveProfiles("test")
@Slf4j
public class UserServiceUnitTest {

    @Autowired
    private  UserService userService;
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserExitReasonRepository userExitReasonRepository;

    @Autowired
    private GradeRepository gradeRepository;

    @Autowired
    GreenRoomRepository greenRoomRepository;

    @Test
    public void hardDeleteTest(){


        User user = User.createUser(new SignupRequestDto("test@gmail.com","!123456"),gradeRepository.findById(1L).orElse(null));
        userRepository.save(user);

        User savedUser = userRepository.findByEmail("test@gmail.com").orElse(null);

        greenRoomRepository.save(GreenRoom.builder().user(savedUser).build());

        //90일 지났다고 가정하고 삭제
        userService.deleteAllWithUser(user);


        assertFalse(userRepository.existsByEmail("test@gmail.com"));
        assertTrue(greenRoomRepository.findAll().isEmpty());
        assertTrue(userRepository.findUserByEmailAndUserStatus("test@gmail.com",UserStatus.DELETE_PENDING).isEmpty());
    }

    @Test
    @Transactional
    public void hardDeleteTest2(){

        User user = User.createUser(new SignupRequestDto("test@gmail.com","!123456"),gradeRepository.findById(1L).orElse(null));
        userRepository.save(user);
        greenRoomRepository.save(GreenRoom.builder().user(user).build());

        UserExitRequestDto userExitRequestDto =  UserExitRequestDto.builder().reasonIdList(List.of()).customReason("별로임 그냥").build();
        userService.deactivateUser(Objects.requireNonNull(user).getEmail(),userExitRequestDto);

        userService.deleteUserHard();

        //90일 안 지났으니까 삭제x
        assertTrue(userRepository.findUserByEmailAndUserStatus("test@gmail.com",UserStatus.DELETE_PENDING).isPresent());
        assertFalse(greenRoomRepository.findAllByUser(user).isEmpty());
    }


    @Test
    @Transactional
    public void softDeleteTest(){

        List<Long> reasonIdList = new ArrayList<>();
        reasonIdList.add(1L); reasonIdList.add(2L);

        UserExitRequestDto userExitRequestDto =  UserExitRequestDto.builder().reasonIdList(reasonIdList).customReason("별로임 그냥").build();
        User user = User.createUser(new SignupRequestDto("test@gmail.com","!123456"),gradeRepository.findById(1L).orElse(null));
        userRepository.save(user);
        userService.deactivateUser(Objects.requireNonNull(user).getEmail(),userExitRequestDto);

        String reason = userExitReasonRepository.findAll().stream().filter(u->u.getReasonType().equals(UserExitReasonType.CUSTOM)).toList().get(0).getReason();

        assertEquals(userExitReasonRepository.findById(1L).get().getCount(),1);
        assertEquals(userExitReasonRepository.findById(2L).get().getCount(),1);
        assertEquals(reason,"별로임 그냥");
    }

}
