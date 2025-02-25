package com.greenroom.server.api.global.exception;

import com.greenroom.server.api.global.response.enums.ResponseCodeEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CustomDataException extends RuntimeException{
    private final ResponseCodeEnum responseCodeEnum;
    private Object data;
}
