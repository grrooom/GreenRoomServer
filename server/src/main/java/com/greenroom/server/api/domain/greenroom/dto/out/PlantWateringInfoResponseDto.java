package com.greenroom.server.api.domain.greenroom.dto.out;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PlantWateringInfoResponseDto {
    private Long plantId;
    private String plantName;
    private String wateringInfo;
}
