package com.greenroom.server.api.domain.greenroom.enums;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;

@Getter
public enum ItemType {

    SHAPE("형태",1),
    ACCESSORY("악세서리",2),
    BACKGROUND("배경",3);

    private final String description;
    private final Integer id;

    ItemType(String description, Integer id) {
        this.description = description;
        this.id = id;
    }
}
