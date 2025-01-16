package com.greenroom.server.api.enums;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;

@Data
@Getter
@Builder
public class ResponseCodeResponseDto {

    String code;
    Integer statusCode;
    String status;
    String reason;
    String postProcess;

    public static ResponseCodeResponseDto from(ResponseCodeEnum responseCodeEnum){
        return ResponseCodeResponseDto.builder()
                .code(responseCodeEnum.getCode())
                .reason(responseCodeEnum.getMessage())
                .postProcess(responseCodeEnum.getPostProcess())
                .statusCode(responseCodeEnum.getStatus().value())
                .status(responseCodeEnum.getStatus().name())
                .build();
    }

}
