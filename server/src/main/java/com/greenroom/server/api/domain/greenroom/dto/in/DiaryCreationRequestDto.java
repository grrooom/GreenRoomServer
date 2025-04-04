package com.greenroom.server.api.domain.greenroom.dto.in;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DiaryCreationRequestDto {
    @NotBlank
    @Size(min = 1,max = 85,message = "제목은 최소 1자 이상, 65자 이하로 입력해주세요.")
    private final String title;

    @NotBlank
    @Size(min = 1,max = 85,message = "본문은 최소 1자 이상, 500자 이하로 입력해주세요.")
    private final String content;

    @NotBlank
    private final String date;
}
