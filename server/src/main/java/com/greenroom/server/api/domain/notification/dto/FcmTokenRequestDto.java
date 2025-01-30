package com.greenroom.server.api.domain.notification.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Data
@Getter
@RequiredArgsConstructor
@AllArgsConstructor
public class FcmTokenRequestDto {

    @NotBlank(message = "fcm token이 비어있음.")
    String fcmToken;
}
