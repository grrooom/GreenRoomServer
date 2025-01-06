package com.greenroom.server.api.domain.greenroom.enums;

import lombok.Getter;

@Getter
public enum ActivityName {

    WATERING("물주기"),
    REPOT("분갈이"),
    PRUNING("가지치기"),
    NUTRITION("영양관리"),
    VENTILATION("환기"),
    SPRAY("분무하기");

    private final String description;

    ActivityName(String description) {
        this.description = description;
    }
}