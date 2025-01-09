package com.greenroom.server.api.security.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

public class EmailAuthenticationDto {

    @Data
    @AllArgsConstructor
    @Getter
    public static class EmailAuthDto{
        String redirectUrl;
        String email;
    }

    @Data
    @AllArgsConstructor
    @Getter
    public static class EmailTokenAuthDto{
        String token;
    }
}
