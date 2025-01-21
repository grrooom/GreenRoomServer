package com.greenroom.server.api.domain.alram.entity;

import com.greenroom.server.api.domain.common.entity.BaseTime;
import com.greenroom.server.api.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(name = "alarm")
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Alarm extends BaseTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long alarmId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private Boolean isNotified;

    private String fcmToken;

    @Builder
    public Alarm(User user){
        this.isNotified = Boolean.FALSE;
        this.user = user;
    }

    public Alarm updateAlarmSet(Boolean todoAlarm){
        this.isNotified = todoAlarm;
        return this;
    }

    public Alarm updateFcmToken(String fcmToken){
        this.fcmToken = fcmToken;
        return this;
    }
}
