package com.greenroom.server.api.domain.user.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;

@Getter
public enum Provider {

    GOOGLE("google"),
    KAKAO("kakao"),
    APPLE("apple"),
    EMAIL("email");

    private final String description;

    Provider(String description) {
        this.description = description;
    }

    @JsonCreator
    public static Provider from(String provider) {
        return Provider.valueOf(provider.toUpperCase());
    }
}
