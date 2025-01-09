package com.greenroom.server.api.domain.greenroom.enums;

import lombok.Getter;

@Getter
public enum LevelIncreasingCause {


    ATTENDANCE("출석"),
    TODO_COMPLETION("할 일 완료"),
    FIRST_GREENROOM_REGISTRATION("첫 식물 등록");

    private final String description;

    LevelIncreasingCause(String description){
        this.description = description;
    }
}
