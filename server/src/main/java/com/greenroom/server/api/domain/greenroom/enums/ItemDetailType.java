package com.greenroom.server.api.domain.greenroom.enums;

import lombok.Getter;

@Getter
public enum ItemDetailType {

    //식물 모양
    BASIC("기본",1),
    FOLIAGE_PLANT("관엽식물",2),
    VINE_PLANT("덩굴식물",3),
    SUCCULENT_CACTUS("다육이,선인장",4),
    HERB("허브",5),
    BULBOUS_PLANT("구근식물",6),
    ANNUAL_PLANT("한해살이풀",7),
    TUBEROUS_PLANT("괴근 식물",8),
    EPIPHYTIC_PLANT("착생식물",9),

    //악세서리
    HAIR_ACCESSORY("헤어핀",10),
    EYEWEAR("안경",11),

    // 배경 소품
    WINDOW_STUFF("창문 소품",12),
    SHELF_STUFF("선반 소품",13);

    private final String description;
    private final Integer id;

    ItemDetailType(String description, Integer id) {
        this.description = description;
        this.id =id;
    }
}
