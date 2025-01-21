package com.greenroom.server.api.domain.user.controller;

import com.greenroom.server.api.domain.user.dto.UserExitRequestDto;
import com.greenroom.server.api.domain.user.service.UserService;
import com.greenroom.server.api.enums.ResponseCodeEnum;
import com.greenroom.server.api.utils.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    @DeleteMapping("")
    public ResponseEntity<ApiResponse> deactivateUser (@AuthenticationPrincipal User user, @RequestBody UserExitRequestDto userExitRequestDto){
        userService.deactivateUser(user.getUsername(), userExitRequestDto);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(null);
    }

    @GetMapping("/exitReasons")
    public ResponseEntity<ApiResponse> getDefinedExitReasons(){
        return ResponseEntity.ok().body(ApiResponse.success(userService.getDefinedUserExitReasons()));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse> logout(@AuthenticationPrincipal User user){
        userService.logout(user.getUsername());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(null);
    }


}
