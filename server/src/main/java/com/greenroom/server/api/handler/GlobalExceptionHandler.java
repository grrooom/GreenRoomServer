package com.greenroom.server.api.handler;

import com.greenroom.server.api.enums.ResponseCodeEnum;
import com.greenroom.server.api.exception.CustomException;
import com.greenroom.server.api.utils.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;


@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({CustomException.class})
    public ResponseEntity<ApiResponse> handleCustomException(CustomException e){
        log.error("[{}] code:{} / code message:{}", e.getResponseCodeEnum().name(),e.getResponseCodeEnum().getCode(), e.getMessage());
        return ResponseEntity.status(e.getResponseCodeEnum().getStatus()).body(ApiResponse.failed(e.getResponseCodeEnum()));

    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse>  handleIllegalArgumentException(IllegalArgumentException e){
        log.error("[Exception] code : {}  code message : {}", ResponseCodeEnum.INVALID_REQUEST_PARAMETER.getCode(), e.getMessage());

        return ResponseEntity.status(ResponseCodeEnum.INVALID_REQUEST_PARAMETER.getStatus()).body(ApiResponse.failed(ResponseCodeEnum.INVALID_REQUEST_PARAMETER));

    }

    @ExceptionHandler({
            UsernameNotFoundException.class
    })
    public ResponseEntity<ApiResponse>  userNotFound(UsernameNotFoundException e) {

        log.error("[Exception] code : {}  code message : {}", ResponseCodeEnum.USER_NOT_FOUND.getCode(), e.getMessage());

        return ResponseEntity.status(ResponseCodeEnum.USER_NOT_FOUND.getStatus()).body(ApiResponse.failed(ResponseCodeEnum.USER_NOT_FOUND));

    }

    // multipartFile 업로드 크기 제한 exception handler
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse>  handleMaxSizeException(MaxUploadSizeExceededException e) {
        log.error("[Exception] code : {}  code message : {}", ResponseCodeEnum.INVALID_IMAGE_FORMAT.getCode(), e.getMessage());
        return ResponseEntity.status(ResponseCodeEnum.INVALID_IMAGE_FORMAT.getStatus()).body(ApiResponse.failed(ResponseCodeEnum.INVALID_IMAGE_FORMAT));

    }

    //기타 처리되지 못한 exception
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse> handlingException(Exception e) {

        log.error("[Exception] code : {}  code message : {}", ResponseCodeEnum.UNKNOWN_SERVER_ERROR.getCode(), e.getMessage());

        return ResponseEntity.status(ResponseCodeEnum.UNKNOWN_SERVER_ERROR.getStatus()).body(ApiResponse.failed(ResponseCodeEnum.UNKNOWN_SERVER_ERROR));

    }

}
