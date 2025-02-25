package com.greenroom.server.api.security.exception;

import com.greenroom.server.api.global.response.enums.ResponseCodeEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class JWTCustomException  extends RuntimeException{
    private ResponseCodeEnum responseCodeEnum;
    private String message;

}
