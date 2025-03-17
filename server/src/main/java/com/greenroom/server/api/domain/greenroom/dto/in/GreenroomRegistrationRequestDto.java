package com.greenroom.server.api.domain.greenroom.dto.in;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.checkerframework.checker.units.qual.N;

import java.time.LocalDate;

@Data
@AllArgsConstructor
public class GreenroomRegistrationRequestDto {

    private Long plantId;

    @NotBlank
    private String nickname;

    private String wateringBaseDate;

    @NotNull(message = "물주는 주기 등록 필수")
    private Integer wateringInterval;

    @NotNull
    private Long itemId;
}
