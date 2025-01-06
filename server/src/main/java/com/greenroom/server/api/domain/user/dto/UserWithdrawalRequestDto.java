package com.greenroom.server.api.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserWithdrawalRequestDto {
    public String withdrawalReason;
}
