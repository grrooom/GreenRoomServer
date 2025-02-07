package com.greenroom.server.api.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserNameUpdateDto {

    @NotBlank(message = "비어있거나 공백인 request argument가 전달됨.")
    private String user_name;
}
