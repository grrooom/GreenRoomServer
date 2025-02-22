package com.greenroom.server.api.domain.greenroom.service;

import com.greenroom.server.api.domain.greenroom.dto.GreenroomInfoResponseDto;
import com.greenroom.server.api.domain.greenroom.dto.GreenroomRegistrationRequestDto;
import com.greenroom.server.api.domain.greenroom.entity.GreenRoom;
import com.greenroom.server.api.domain.greenroom.entity.Todo;
import com.greenroom.server.api.domain.greenroom.repository.TodoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Predicate;

@Service
@RequiredArgsConstructor
public class TodoService {

    private final TodoRepository todoRepository;

    //service
    public final ActivityService activityService;

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
}
