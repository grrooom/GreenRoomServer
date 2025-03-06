package com.greenroom.server.api.domain.greenroom.dto.out;

import com.greenroom.server.api.domain.greenroom.entity.Item;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ItemResponseDTO {

    private Long id;
    private String name;
    private String category;
    private Integer categoryId;
    private String subCategory;
    private Integer subCategoryId;
    private Boolean isUnLocked;
    private Integer unLockLevel;
    @Builder
    private ItemResponseDTO(Long id,String name,String category, String subCategory,Boolean isUnLocked, Integer unLockLevel, Integer categoryId, Integer subCategoryId){
        this.id = id;
        this.name =name;
        this.category = category;
        this.categoryId = categoryId;
        this.subCategoryId = subCategoryId;
        this.subCategory= subCategory;
        this.isUnLocked = isUnLocked;
        this.unLockLevel = unLockLevel;
    }

    public static ItemResponseDTO of(Item item, Boolean isItemUnLocked){

        return ItemResponseDTO.builder()
                .id(item.getItemId())
                .name(item.getItemName())
                .category(item.getItemType().getDescription())
                .categoryId(item.getItemType().getId())
                .subCategory(item.getItemDetailType().getDescription())
                .subCategoryId(item.getItemDetailType().getId())
                .isUnLocked(isItemUnLocked)
                .unLockLevel(item.getGrade().getLevel())
                .build();
    }
}
