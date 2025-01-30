package com.greenroom.server.api.domain.notification.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@AllArgsConstructor
@Data
public class NotificationEnabledUpdateRequestDto {

    @NotNull(message = "알림 수신 여부가 비어있음.")
    private Boolean notification_enabled;
}
