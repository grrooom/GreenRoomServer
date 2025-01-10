package com.greenroom.server.api.security.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TokenRequestDto {

    @NotBlank(message = "비어있는 token 갱신 용 refresh token을 전달 받음.")
    private String refreshToken;
}
