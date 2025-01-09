package com.greenroom.server.api.exception;

import com.greenroom.server.api.enums.ResponseCodeEnum;
import lombok.Getter;

@Getter
public class CustomException extends RuntimeException{

    private final ResponseCodeEnum responseCodeEnum ;

    public CustomException(ResponseCodeEnum code, String message) {
        super(message);
        this.responseCodeEnum = code;
    }

    public CustomException(ResponseCodeEnum code) {
        super(" ");
        this.responseCodeEnum = code;
    }


}
