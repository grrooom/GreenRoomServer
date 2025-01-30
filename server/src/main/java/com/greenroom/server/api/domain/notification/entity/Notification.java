package com.greenroom.server.api.domain.notification.entity;

import com.greenroom.server.api.domain.common.entity.BaseTime;
import com.greenroom.server.api.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Table(name = "notification")
@Entity
@Getter
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
public class Notification extends BaseTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long notificationId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private Boolean notificationEnabled;

    private String fcmToken;

    public static Notification createAlarm(User user, String fcmToken ){
        return Notification.builder().user(user).fcmToken(fcmToken).notificationEnabled(true).build();
    }
    public void updateNotificationEnabled(Boolean notificationEnabled){
        this.notificationEnabled= notificationEnabled;
    }

    public void updateFcmToken(String fcmToken){
        this.fcmToken = fcmToken;
    }
}
