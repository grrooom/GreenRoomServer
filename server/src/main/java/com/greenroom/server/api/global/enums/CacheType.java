package com.greenroom.server.api.global.enums;

import lombok.Getter;

@Getter
public enum CacheType {
    ITEM("items",12*60*60,50),
    PLANT("plants",60*60,10);

    private final String cacheName;
    private final Integer expiredAfterWrite; // seconds
    private final Integer maximumSize;

    CacheType(String cacheName, Integer expiredAfterWrite, Integer maximumSize){
        this.cacheName = cacheName;
        this.expiredAfterWrite = expiredAfterWrite;
        this.maximumSize = maximumSize;
    }
}
