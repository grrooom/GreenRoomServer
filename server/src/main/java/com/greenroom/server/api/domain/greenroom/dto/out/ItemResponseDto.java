package com.greenroom.server.api.domain.greenroom.dto.out;

import com.greenroom.server.api.domain.greenroom.entity.Item;

public record ItemResponseDto(
        Long id,
        String name,
        String category,
        Integer categoryId,
        String subCategory,
        Integer subCategoryId,
        Boolean isUnLocked,
        Integer unLockLevel){

    public static ItemResponseDto of(Item item, Boolean isItemUnLocked){
        return new ItemResponseDto(item.getItemId(),item.getItemName(),item.getItemType().getDescription(), item.getItemType().getId(),item.getItemDetailType().getDescription(),item.getItemDetailType().getId(),isItemUnLocked,item.getGrade().getLevel());
    }
}
