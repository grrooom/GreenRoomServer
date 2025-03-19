package com.greenroom.server.api.domain.greenroom.service;

import com.greenroom.server.api.domain.greenroom.entity.Activity;
import com.greenroom.server.api.domain.greenroom.repository.ActivityRepository;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.units.qual.A;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ActivityService {
    private final ActivityRepository activityRepository;

    public Activity getWateringActivity(){
        return activityRepository.findActivityByActivityName("watering").orElse(null);
    }

    public List<Activity> findAllActivity(){
        return activityRepository.findAll();
    }

    public List<Activity> findAllByIds(List<Long> activityList){
        return activityRepository.findAllById(activityList);
    }
}
