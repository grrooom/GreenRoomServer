package com.greenroom.server.api.domain.greenroom.dto.in;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Data
@AllArgsConstructor
public class GreenroomImageSimpleDto {
    private Long greenroomId;
    private String pictureUrl;
}
