package com.greenroom.server.api.domain.greenroom.controller;

import com.google.protobuf.Api;
import com.greenroom.server.api.domain.greenroom.dto.in.*;
import com.greenroom.server.api.domain.greenroom.dto.out.PointAndLevelUpResponseDto;
import com.greenroom.server.api.domain.greenroom.service.GreenroomService;
import com.greenroom.server.api.global.response.enums.ResponseCodeEnum;
import com.greenroom.server.api.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.checkerframework.checker.units.qual.A;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("api/greenroom")
@RequiredArgsConstructor
public class GreenroomController {

    private final GreenroomService greenroomService;

    @GetMapping("/info")
    public ResponseEntity<ApiResponse> getUserGreenroomInfo(@AuthenticationPrincipal User user){
        return ResponseEntity.ok().body(ApiResponse.success(greenroomService.getGreenroomInfo(user.getUsername())));

    }

    //닉네임 중복 확인
    @GetMapping("/nickname/duplication")
    public ResponseEntity<ApiResponse> checkNicknameValidation(@AuthenticationPrincipal User user,@RequestParam(value = "nickname")String nickname){

        return ResponseEntity.ok().body(ApiResponse.success(greenroomService.checkDuplication(user.getUsername(), nickname)));
    }

    // 그린룸 등록
    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse> createGreenroom(@AuthenticationPrincipal User user, @Valid @RequestPart(value = "data") GreenroomRegistrationRequestDto greenroomRegistrationRequestDto, @RequestPart(value = "imageFile",required = false) MultipartFile imageFile){
        PointAndLevelUpResponseDto pointAndLevelUpResponseDto =  greenroomService.createGreenroom(user.getUsername(), greenroomRegistrationRequestDto,imageFile);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(ResponseCodeEnum.CREATED,pointAndLevelUpResponseDto));

    }

    @PostMapping("/{greenroom_id}/todo/completion")
    public ResponseEntity<ApiResponse> completeTodo(@PathVariable(value = "greenroom_id")Long greenroomId, @RequestBody @Valid CompleteTodoRequestDto completeTodoRequestDto){
        return ResponseEntity.ok(ApiResponse.success(ResponseCodeEnum.SUCCESS,greenroomService.completeTodo(greenroomId,completeTodoRequestDto)));
    }

    @PatchMapping("/{greenroom_id}/items")
    public ResponseEntity<ApiResponse> decoratePlant(@Valid@RequestBody GreenroomDecorationRequestDto greenroomDecorationRequestDto, @PathVariable(value = "greenroom_id") Long greenroomId){
        return ResponseEntity.ok(ApiResponse.success(ResponseCodeEnum.SUCCESS,greenroomService.updateGreenroomAdornment(greenroomId, greenroomDecorationRequestDto)));
    }

    @GetMapping("/{greenroom_id}/details")
    public ResponseEntity<ApiResponse> getGreenroomDetails(@PathVariable(value = "greenroom_id") Long greenroomId){
        return ResponseEntity.ok(ApiResponse.success(ResponseCodeEnum.SUCCESS,greenroomService.getGreenroomDetails(greenroomId)));
    }

    @DeleteMapping("/{greenroom_id}")
    public ResponseEntity<ApiResponse> deleteGreenroom(@PathVariable(value = "greenroom_id") Long greenroomId){
        greenroomService.deleteAllGreenroomAndDeleteAllImages(List.of(greenroomId));
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(null);
    }

    @PatchMapping("{greenroom_id}/memo")
    public ResponseEntity<ApiResponse> postMemo(@PathVariable(value = "greenroom_id") Long greenroomId, @RequestBody MemoRequestDto memoRequestDto){
        greenroomService.postMemo(greenroomId,memoRequestDto);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(null);
    }

    @PatchMapping("/{greenroom_id}/plant")
    public ResponseEntity<ApiResponse> postGreenroomPlant(@PathVariable(value = "greenroom_id") Long greenroomId, @RequestBody GreenroomPlantRequestDto greenroomPlantRequestDto){
        return ResponseEntity.ok(ApiResponse.success(ResponseCodeEnum.SUCCESS,greenroomService.registerGreenroomPlant(greenroomId,greenroomPlantRequestDto)));
    }

}
