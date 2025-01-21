package com.greenroom.server.api.domain.user.dto;

import lombok.*;

import java.util.List;

@Data
@Getter
@Builder
@RequiredArgsConstructor
@AllArgsConstructor
public class UserExitRequestDto {

    private List<Long> reasonIdList;

    private String customReason;
}
