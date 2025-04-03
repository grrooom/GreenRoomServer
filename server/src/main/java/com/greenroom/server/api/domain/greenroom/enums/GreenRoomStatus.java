package com.greenroom.server.api.domain.greenroom.enums;

import lombok.Getter;

@Getter
public enum GreenRoomStatus {

    ENABLED("활성화됨"),
    DISABLED("비활성화됨");

    private final String description;

    GreenRoomStatus(String description) {
        this.description = description;
    }
}
