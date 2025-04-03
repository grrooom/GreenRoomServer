package com.greenroom.server.api.domain.greenroom.service;

import com.greenroom.server.api.domain.greenroom.entity.GreenRoom;
import com.greenroom.server.api.domain.greenroom.entity.TodoLog;
import com.greenroom.server.api.domain.greenroom.repository.TodoLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TodoLogService {

    private final TodoLogRepository todoLogRepository;

    public void createTodoLog(List<TodoLog> todoLogList){
        if(!todoLogList.isEmpty()){
            todoLogRepository.saveAll(todoLogList);
        }
    }

    public void deleteAllByGreenroom(List<Long> greenroomIdList){
        todoLogRepository.deleteAllByGreenRoom(greenroomIdList);
    }

    public List<TodoLog> getAllTodoLogByGreenroomAndDate(List<GreenRoom> greenRooms, LocalDate date){
        return todoLogRepository.findByCreateDateAndGreenRoomIn(date, greenRooms.stream().map(GreenRoom::getGreenroomId).toList());
    }
}
