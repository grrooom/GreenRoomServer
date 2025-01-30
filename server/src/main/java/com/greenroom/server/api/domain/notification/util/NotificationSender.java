package com.greenroom.server.api.domain.notification.util;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationSender {

    public void sendAlarm(String fcmToken)  {

        Message message = Message.builder()
                .setNotification(Notification.builder()
                        .setTitle("fcm test title")
                        .setBody("fcm test body")
                        .build())
                .setToken(fcmToken)
                .build();
        try {
            FirebaseMessaging.getInstance().send(message);
        }catch (FirebaseMessagingException e){
            log.error("[EXCEPTION] : fail to send fcm notification");
        }
    }

}
