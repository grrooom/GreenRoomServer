package com.greenroom.server.api.domain.greenroom.dto.in;

import java.util.List;

public record ActivityStatusUpdateRequestDto(List<Long> activeList, List<Long> inactiveList) {
}
