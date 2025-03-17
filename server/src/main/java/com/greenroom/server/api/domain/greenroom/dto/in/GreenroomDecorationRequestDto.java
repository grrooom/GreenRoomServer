package com.greenroom.server.api.domain.greenroom.dto.in;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GreenroomDecorationRequestDto {
    @NotNull
    private Long shape;
    private Long hairAccessory;
    private Long eyewear;
    private Long window ;
    private Long shelf;
}
