package com.greenroom.server.api.security.controller;


import com.greenroom.server.api.enums.ResponseCodeEnum;
import com.greenroom.server.api.security.dto.*;
import com.greenroom.server.api.security.service.CustomUserDetailService;
import com.greenroom.server.api.utils.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final CustomUserDetailService userDetailService;

    //access token & refresh token update (Refresh Token Rotation)
    @PutMapping("/tokens")
    public ResponseEntity<ApiResponse> updateToken(@Valid @RequestBody TokenRequestDto tokenRequestDto){
        String refreshToken = tokenRequestDto.getRefreshToken();
        TokenDto tokenDto = userDetailService.updateToken(refreshToken);
        return ResponseEntity.ok().body(ApiResponse.success(ResponseCodeEnum.SUCCESS, tokenDto));
    }

    //이메일 회원가입
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse> signup( @Valid @RequestBody SignupRequestDto signupRequestDto){
        userDetailService.save(signupRequestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(ResponseCodeEnum.CREATED));
    }

   //일반 로그인
    @PostMapping("/login")
    public ResponseEntity<ApiResponse> login(@Valid @RequestBody LoginRequestDto loginRequestDto){

        TokenDto tokenDto =  userDetailService.login(loginRequestDto.getEmail(),loginRequestDto.getPassword());
        return ResponseEntity.ok().body(ApiResponse.success(ResponseCodeEnum.SUCCESS,tokenDto));
    }


    @PutMapping("/email/authentication")
    public ResponseEntity<ApiResponse> authenticateEmail(@Valid @RequestBody EmailAuthenticationDto.EmailAuthDto authenticationDto){
        userDetailService.emailAuthentication(authenticationDto.getRedirectUrl(),authenticationDto.getEmail());
        return ResponseEntity.ok().body(ApiResponse.success(ResponseCodeEnum.SUCCESS));

    }

    @PutMapping("/email/token/authentication")
    public ResponseEntity<ApiResponse> verifyEmailToken(@Valid @RequestBody EmailAuthenticationDto.EmailTokenAuthDto authenticationDto){
        userDetailService.verifyEmailToken( authenticationDto.getToken());
        return ResponseEntity.ok().body(ApiResponse.success(ResponseCodeEnum.SUCCESS));

    }



}
