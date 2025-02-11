package com.greenroom.server.api.domain.greenroom.dto;

import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Data
public class GreenroomImageSimpleDto {
    private Long greenroomId;
    private String pictureUrl;
}
