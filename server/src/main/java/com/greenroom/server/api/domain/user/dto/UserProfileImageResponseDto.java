package com.greenroom.server.api.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserProfileImageResponseDto {
    private String image_url;
}
