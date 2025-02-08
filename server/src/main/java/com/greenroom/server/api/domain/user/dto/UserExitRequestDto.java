package com.greenroom.server.api.domain.user.dto;

import lombok.*;
import org.hibernate.annotations.Array;

import java.util.List;

@Data
@Builder
@RequiredArgsConstructor
@AllArgsConstructor
public class UserExitRequestDto {

    private List<Long> reasonIdList;

    private String customReason;
}
