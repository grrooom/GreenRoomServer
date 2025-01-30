package com.greenroom.server.api.domain.admin;

import com.greenroom.server.api.domain.notification.dto.FcmTokenRequestDto;
import com.greenroom.server.api.domain.notification.service.NotificationService;
import com.greenroom.server.api.enums.ResponseCodeEnum;
import com.greenroom.server.api.utils.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminController {

    private final AdminService adminService;
    private final NotificationService notificationService;
    @DeleteMapping("/data")
    public ResponseEntity<ApiResponse> deleteAllData(){
        adminService.deleteAllData();
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(null);
    }
    @PostMapping("/notification")
    public ResponseEntity<ApiResponse> sendNotification(@RequestBody FcmTokenRequestDto fcmTokenRequestDto ){
        notificationService.sendNotification(fcmTokenRequestDto.getFcmToken());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(null);
    }

}
