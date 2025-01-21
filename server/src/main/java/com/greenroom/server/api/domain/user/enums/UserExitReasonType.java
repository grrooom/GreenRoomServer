package com.greenroom.server.api.domain.user.enums;

import lombok.Getter;

@Getter
public enum UserExitReasonType {

    CUSTOM("custom"),

    DEFINED("defined");

    private final String description;
    UserExitReasonType(String description){
        this.description = description;
    }

}
