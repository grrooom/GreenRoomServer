package com.greenroom.server.api.domain.greenroom.dto.out;

import com.greenroom.server.api.domain.greenroom.entity.Item;

public record ItemSimpleDto(Long itemId, String itemName){
    public static ItemSimpleDto from(Item item){
        return new ItemSimpleDto(item.getItemId(),item.getItemName());
    }
}