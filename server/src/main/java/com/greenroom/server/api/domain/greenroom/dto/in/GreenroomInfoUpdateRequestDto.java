package com.greenroom.server.api.domain.greenroom.dto.in;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record GreenroomInfoUpdateRequestDto (

        @NotBlank
        String nickname,

        @NotNull
        Boolean isAlive,

        @NotNull
        ImageAction imageAction){
    public enum ImageAction{
        DELETE(),
        UPLOAD(),
        NONE();
    }
}
