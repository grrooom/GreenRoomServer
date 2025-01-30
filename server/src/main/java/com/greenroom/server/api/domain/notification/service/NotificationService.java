package com.greenroom.server.api.domain.notification.service;

import com.greenroom.server.api.domain.notification.entity.Notification;
import com.greenroom.server.api.domain.notification.repository.NotificationRepository;
import com.greenroom.server.api.domain.notification.util.NotificationSender;
import com.greenroom.server.api.domain.user.entity.User;
import com.greenroom.server.api.enums.ResponseCodeEnum;
import com.greenroom.server.api.exception.CustomException;
import com.greenroom.server.api.security.service.CustomUserDetailService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final CustomUserDetailService customUserDetailService;
    private final NotificationSender notificationSender;

    @Transactional
    public void createNotification(String email,String fcmToken){

        //UserNotFound, INVALID_REQUEST_ARGUMENT

        User user =customUserDetailService.findUserByEmail(email); //user 없으면 error 반환

        Optional<Notification> alarm = notificationRepository.findByUser(user);

        if(alarm.isPresent()){alarm.get().updateFcmToken(fcmToken);} //이미 있으면 token update

        else{
            notificationRepository.save(Notification.createAlarm(user,fcmToken));} //없으면 새로 생성.
    }

    @Transactional
    public void updateNotificationEnabled(String email, Boolean notificationEnabled){

        //UserNotFound, FCM_TOKEN_NOT_FOUND,  INVALID_REQUEST_ARGUMENT

        User user =customUserDetailService.findUserByEmail(email); //user 없으면 error 반환

        Optional<Notification> notification = notificationRepository.findByUser(user);

        //알림 수신을 선택했는데, 객체 존재하지 않거나, fcm token만 비어있을 경우 error 반환
        if(notificationEnabled &&(notification.isEmpty() ||notification.get().getFcmToken().isBlank())){throw new CustomException(ResponseCodeEnum.FCM_TOKEN_NOT_FOUND);}

        //정상일 경우 수신 여부 update
        notification.ifPresent(n -> n.updateNotificationEnabled(notificationEnabled));

    }

    public void sendNotification(String fcmToken){
        notificationSender.sendAlarm(fcmToken);
    }

}
