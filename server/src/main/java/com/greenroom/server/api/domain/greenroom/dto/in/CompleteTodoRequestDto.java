package com.greenroom.server.api.domain.greenroom.dto.in;


import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class CompleteTodoRequestDto {

    @NotEmpty
    @NotNull
    private List<Long> completedTodo;
}
