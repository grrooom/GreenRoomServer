package com.greenroom.server.api.security.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Getter
@AllArgsConstructor
@RequiredArgsConstructor
public class LoginRequestDto {

    @Email(message = "email형식과 일치하지 않음.")
    @NotBlank(message = "비어 있는 로그인 email을 전달 받음.")
    private String email;
    @NotBlank(message = "비어 있는 로그인 password를 전달 받음.")
    private String password;

}
