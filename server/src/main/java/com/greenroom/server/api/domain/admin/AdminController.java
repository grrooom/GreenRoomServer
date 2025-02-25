package com.greenroom.server.api.domain.admin;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.greenroom.server.api.domain.notification.dto.FcmTokenRequestDto;
import com.greenroom.server.api.domain.notification.service.NotificationService;
import com.greenroom.server.api.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;
    private final NotificationService notificationService;

    // 고정 data 제외 전부 삭제
    @DeleteMapping("/data")
    public ResponseEntity<ApiResponse> deleteAllData(){
        adminService.deleteAllData();
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(null);
    }

    //특정 user 삭제 -> user 관련 전부 삭제
    @DeleteMapping("/data/user")
    public ResponseEntity<ApiResponse> deleteUsers(@RequestParam(value = "email") String email){
        log.info(email);
        adminService.deleteSpecificUser(email);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(null);
    }

    //-> 푸시 알림 전송 테스트
    @PostMapping("/notification")
    public ResponseEntity<ApiResponse> sendNotification(@RequestBody FcmTokenRequestDto fcmTokenRequestDto ){
        notificationService.sendNotification(fcmTokenRequestDto.getFcmToken());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(null);
    }

    //공공 api -> 식물 데이터 저장
    @PostMapping("/plants")
    public void getPlantInfo() throws JsonProcessingException {
        adminService.insertPlantDataIntoDB();
    }

    //식물 전체 데이터 삭제
    @DeleteMapping("/plants")
    public void deleteAllPlantData (){
        adminService.deleteAllPlantData();
    }


    //식물 데이터 사진을 s3 저장 -> object key 저장
    @PostMapping("/plants/image")
    public void uploadImagesToS3() throws IOException {
        adminService.uploadPlantImageToS3();
    }

    //rdb를 elasticsearch와 동기화
    @PostMapping("/plant-document")
    public void updatePlantDocumentWithPlant(){
        adminService.updatePlantDocumentWithPlant();
    }



}
