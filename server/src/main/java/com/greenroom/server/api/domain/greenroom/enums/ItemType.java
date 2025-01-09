package com.greenroom.server.api.domain.greenroom.enums;

import lombok.Getter;

@Getter
public enum ItemType {

    SHAPE("모양"),
    HAIR_ACCESSORY("머리핀"),
    GLASSES("안경"),
    BACKGROUND_WINDOW("창문배경"),
    BACKGROUND_SHELF("선반배경");

    private final String description;

    ItemType(String description) {
        this.description = description;
    }
}
