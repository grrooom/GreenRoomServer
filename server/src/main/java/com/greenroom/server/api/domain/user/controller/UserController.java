package com.greenroom.server.api.domain.user.controller;

import com.greenroom.server.api.domain.user.dto.UserExitRequestDto;
import com.greenroom.server.api.domain.user.dto.UserNameUpdateDto;
import com.greenroom.server.api.domain.user.dto.UserProfileImageResponseDto;
import com.greenroom.server.api.domain.user.service.CheckInService;
import com.greenroom.server.api.domain.user.service.GradeService;
import com.greenroom.server.api.domain.user.service.UserService;
import com.greenroom.server.api.global.response.enums.ResponseCodeEnum;
import com.greenroom.server.api.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final CheckInService checkInService;

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

    @GetMapping("/info")
    public ResponseEntity<ApiResponse> getUserInformation(@AuthenticationPrincipal User user){
        return ResponseEntity.ok().body(ApiResponse.success(userService.getUserInformation(user.getUsername())));
    }

    @PatchMapping("/name")
    public ResponseEntity<ApiResponse> updateUserName(@AuthenticationPrincipal User user, @Valid @RequestBody UserNameUpdateDto userNameUpdateDto) {
        return ResponseEntity.ok().body(ApiResponse.success(userService.updateUserName(user.getUsername(), userNameUpdateDto.getUser_name())));
    }

    @DeleteMapping("/profile-image")
    public ResponseEntity<ApiResponse> deleteUserProfileImage(@AuthenticationPrincipal User user){
        userService.deleteUserProfileImage(user.getUsername());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(null);
    }

    @PostMapping("/profile-image")
    public ResponseEntity<ApiResponse> uploadProfileImage(@AuthenticationPrincipal User user, @RequestPart MultipartFile profile_image_file){
        UserProfileImageResponseDto imageURl = userService.uploadUserProfileImage(user.getUsername(),profile_image_file);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(ResponseCodeEnum.CREATED,imageURl));
    }

    @PostMapping("/check-in")
    public ResponseEntity<ApiResponse> checkIn(@AuthenticationPrincipal User user){
        return ResponseEntity.ok(ApiResponse.success(ResponseCodeEnum.SUCCESS,checkInService.doCheckIn(user.getUsername())));
    }

}
