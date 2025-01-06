package com.greenroom.server.api.domain.alram.service;

import com.greenroom.server.api.domain.alram.entity.Alarm;
import com.greenroom.server.api.domain.alram.repository.AlarmRepository;
import com.greenroom.server.api.domain.user.dto.MyPageDto;
import com.greenroom.server.api.domain.user.entity.User;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AlarmService {

    private final AlarmRepository alarmRepository;
    public Alarm findAlarmByUser(User user){
        return alarmRepository.findByUser(user)
                .orElse(null);
    }

    @Transactional
    public void updateAlarm(User user, MyPageDto.MyPageAlarm dto){

        Alarm alarm = findAlarmByUser(user)
                .updateAlarmSet(dto.getTodoAlarm());
        alarmRepository.save(alarm);
    }
}
