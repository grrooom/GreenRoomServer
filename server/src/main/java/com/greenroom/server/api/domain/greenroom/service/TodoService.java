package com.greenroom.server.api.domain.greenroom.service;

import com.greenroom.server.api.domain.greenroom.dto.out.GreenroomInfoResponseDto;
import com.greenroom.server.api.domain.greenroom.dto.out.PointAndLevelUpResponseDto;
import com.greenroom.server.api.domain.greenroom.entity.GreenRoom;
import com.greenroom.server.api.domain.greenroom.entity.Todo;
import com.greenroom.server.api.domain.greenroom.entity.TodoLog;
import com.greenroom.server.api.domain.greenroom.repository.TodoRepository;
import com.greenroom.server.api.domain.user.entity.User;
import com.greenroom.server.api.domain.user.service.GradeService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

@Service
@RequiredArgsConstructor
public class TodoService {

    private final TodoRepository todoRepository;

    //service
    public final ActivityService activityService;
    public final TodoLogService todoLogService;
    public final GradeService gradeService;

    public GreenroomInfoResponseDto.GreenroomTodoInfoDto getGreenroomTodoInfo(GreenRoom greenRoom){

        List<Todo> greemroomTodoList = todoRepository.findAllByGreenRoomAndUseYn(greenRoom,true);

        //오늘 해야하는 일인지 확인
        Predicate<Todo> isTodo = todo -> !todo.getNextTodoDate().isAfter(LocalDate.now());

        List<GreenroomInfoResponseDto.TodoSimpleDto> todoSimpleDtoList =  greemroomTodoList.stream().filter(isTodo).map(todo->GreenroomInfoResponseDto.TodoSimpleDto.from(todo.getActivity())).toList();

        return new GreenroomInfoResponseDto.GreenroomTodoInfoDto(todoSimpleDtoList, todoSimpleDtoList.size());

    }

    public void createWateringTodo(Integer wateringInterval,GreenRoom greenRoom, LocalDate wateringBaseDate){

        Todo todo =Todo.builder()
                .baseDate(wateringBaseDate)
                .term(wateringInterval)
                .greenRoom(greenRoom)
                .activity(activityService.getWateringActivity())
                .nextTodoDate(wateringBaseDate.plusDays(wateringInterval))
                .build();
        todoRepository.save(todo);
    }

    @Transactional
    public PointAndLevelUpResponseDto completeTodo(GreenRoom greenRoom, List<Long> activityIdList){

        User user = greenRoom.getUser();

        int totalPoints = 0;

        List<TodoLog> todoLogList = new ArrayList<>();

        List<Todo> todoList =  todoRepository.findAllByGreenRoomAndActivity(greenRoom.getGreenroomId(),activityIdList);

        //todo update
        for(Todo todo : todoList){
            if(!todo.getNextTodoDate().isAfter(LocalDate.now())&&todo.getUseYn()){
                todo.updateNextTodoDate(LocalDate.now().plusDays(todo.getTerm()));
                totalPoints++;
                todoLogList.add(TodoLog.builder().greenRoom(greenRoom).activity(todo.getActivity()).build());
            }
        }

        //todoLog 생성
        if(!todoLogList.isEmpty()){todoLogService.createTodoLog(todoLogList);}

        user.addTotalSeed(totalPoints);
        return PointAndLevelUpResponseDto.of(greenRoom.getUser(),totalPoints,gradeService.updateUserGrade(user));
    }
}
