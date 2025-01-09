package com.greenroom.server.api.utils;

import com.greenroom.server.api.enums.ResponseCodeEnum;
import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class ApiResponse {
    private String status;
    private String code;
    private Object data;

    @Builder
    public ApiResponse(String status,String code, Object data) {
        this.status = status;
        this.code = code;
        this.data = data;
    }

    //data가 없는 success
    public static ApiResponse success(ResponseCodeEnum responseCodeEnum) {
        return success(responseCodeEnum,null);
    }


    public static ApiResponse success() {
        return success(ResponseCodeEnum.SUCCESS,null);
    }

    public static ApiResponse success(Object data) {
        return success(ResponseCodeEnum.SUCCESS,data);
    }


    public static ApiResponse success(ResponseCodeEnum responseCodeEnum, Object data) {
        return ApiResponse.builder()
                .status(responseCodeEnum.getStatus().name())
                .code(responseCodeEnum.getCode())
                .data(data).build();
    }

    //failed
    public static ApiResponse failed(ResponseCodeEnum responseCodeEnum) {
        return ApiResponse.builder()
                .status(responseCodeEnum.getStatus().name())
                .code(responseCodeEnum.getCode())
                .build();
    }

    //data가 있는 failed
    public static ApiResponse failed(ResponseCodeEnum responseCodeEnum,Object data){
        return ApiResponse.builder()
                .status(responseCodeEnum.getStatus().name())
                .code(responseCodeEnum.getCode())
                .data(data)
                .build();
    }
}
