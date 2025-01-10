package com.greenroom.server.api.security.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Data
@RequiredArgsConstructor
@AllArgsConstructor
public class SignupRequestDto {

    @NotBlank(message = "비어 있는 회원가입 email을 전달 받음.")@Email(message = "email형식과 일치하지 않음.")
    String email;
    @NotBlank(message = "비어 있는 회원가입 password를 전달 받음.")
    String password;
}
