package com.greenroom.server.api.domain.greenroom.dto.in;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ActivityInfoUpdateRequestDto(@Valid List<ActivityInfoUpdateDto> updateList){
    public record ActivityInfoUpdateDto(
            @NotNull
            Long activityId,
            @NotNull
            String date,
            @NotNull
            Integer term){}
}
