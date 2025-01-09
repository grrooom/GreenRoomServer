package com.greenroom.server.api.security.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

public class EmailAuthenticationDto {

    @Data
    @AllArgsConstructor
    @Getter
    public static class EmailAuthDto{
        @NotBlank(message = "전달 받은 딥링크 redirectUrl이 비어있음.")
        String redirectUrl;
        @NotBlank(message = "비어 있는 이메일 인증용 email을 전달 받음.")
        @Email(message = "email형식과 일치하지 않음.")
        String email;
    }

    @Data
    @AllArgsConstructor
    @Getter
    public static class EmailTokenAuthDto{
        @NotBlank(message = "비어 있는 email인증 토큰을 전달받음.")
        String token;
    }
}
