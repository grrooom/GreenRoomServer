package com.greenroom.server.api.domain.user.dto;

import com.greenroom.server.api.domain.user.entity.UserExitReason;
import lombok.*;

@Data
@RequiredArgsConstructor
@AllArgsConstructor
@Builder
@Getter
public class UserExitReasonResponseDto {

    private Long reasonId;
    private String reason;

    public static UserExitReasonResponseDto from(UserExitReason userExitReason){
        return UserExitReasonResponseDto.builder().reasonId(userExitReason.getUserExitReasonId()).reason(userExitReason.getReason()).build();
    }
}
