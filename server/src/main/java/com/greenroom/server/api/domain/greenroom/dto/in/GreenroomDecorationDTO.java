package com.greenroom.server.api.domain.greenroom.dto.in;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GreenroomDecorationDTO {
    @NotNull
    private Long shape;
    private Long hairAccessory;
    private Long eyewear;
    private Long window ;
    private Long shelf;
}
