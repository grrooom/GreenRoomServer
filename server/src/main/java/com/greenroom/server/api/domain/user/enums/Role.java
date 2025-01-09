package com.greenroom.server.api.domain.user.enums;

import lombok.Getter;

@Getter
public enum Role {
    GUEST("손님"),
    GENERAL("일반유저"),
    ADMIN("관리자");

    private final String description;

    Role(String description) {
        this.description = description;
    }
}
