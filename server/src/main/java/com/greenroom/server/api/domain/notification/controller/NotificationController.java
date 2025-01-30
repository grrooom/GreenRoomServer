package com.greenroom.server.api.domain.notification.controller;

import com.greenroom.server.api.domain.notification.dto.FcmTokenRequestDto;
import com.greenroom.server.api.domain.notification.dto.NotificationEnabledUpdateRequestDto;
import com.greenroom.server.api.domain.notification.service.NotificationService;
import com.greenroom.server.api.enums.ResponseCodeEnum;
import com.greenroom.server.api.utils.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/notifications")
public class NotificationController {


    private final NotificationService notificationService;

    @PostMapping("fcmToken")
    public ResponseEntity<ApiResponse> createNotification(@AuthenticationPrincipal User user, @Valid @RequestBody FcmTokenRequestDto fcmTokenRequestDto){
        notificationService.createNotification(user.getUsername(), fcmTokenRequestDto.getFcmToken());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(ResponseCodeEnum.CREATED));
    }

    @PatchMapping
    public ResponseEntity<ApiResponse> updateNotificationEnabled(@AuthenticationPrincipal User user, @Valid @RequestBody NotificationEnabledUpdateRequestDto notificationEnabledUpdateRequestDto){
        notificationService.updateNotificationEnabled(user.getUsername(), notificationEnabledUpdateRequestDto.getNotification_enabled());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(null);
    }

}
