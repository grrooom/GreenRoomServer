package com.greenroom.server.api.domain.greenroom.controller;

import com.greenroom.server.api.domain.greenroom.service.GreenroomService;
import com.greenroom.server.api.utils.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/greenroom")
@RequiredArgsConstructor
public class GreenroomController {

    private final GreenroomService greenroomService;

    @GetMapping("/info")
    public ResponseEntity<ApiResponse> getUserGreenroomInfo(@AuthenticationPrincipal User user){
        return ResponseEntity.ok().body(ApiResponse.success(greenroomService.getGreenroomInfo(user.getUsername())));

    }

}
