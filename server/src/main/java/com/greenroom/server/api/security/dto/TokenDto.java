package com.greenroom.server.api.security.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TokenDto {

    private String email;
    private String accessToken;
    private String refreshToken;
//    private String token;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class ResponseTokenDto{
        private String refreshToken;
        private String accessToken;
    }
}
