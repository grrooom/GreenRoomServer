package com.greenroom.server.api.domain.user.dto;

import com.greenroom.server.api.domain.greenroom.entity.Grade;
import com.greenroom.server.api.domain.greenroom.entity.Item;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

public class MyPageDto {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GradeDto{

        private String description;
        private String gradeImageUrl;
        private int requiredSeedToNextLevel;
        private int currentSeed;
        private int nextLevelSeed;
        private int currentLevel;

        public GradeDto(Grade grade,int requiredSeed,int nextLevelSeed,int currentSeed){
            this.description = grade.getDescription();
            this.gradeImageUrl = grade.getGradeImageUrl();
            this.currentLevel = grade.getLevel();
            this.currentSeed = currentSeed;
            this.nextLevelSeed = nextLevelSeed;
            this.requiredSeedToNextLevel = requiredSeed;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemDto{

        private String itemName;
        private String itemImageUrl;

        public ItemDto(Item item){
            this.itemName = item.getItemName();
            this.itemImageUrl = item.getImageUrl();
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemDtoV2{

        private String itemName;
        private String itemType;

        public ItemDtoV2(Item item){
            this.itemName = item.getItemName();
            this.itemType = item.getItemType().name();
        }
    }

    @Builder
    @Data
    public static class MyPageMainResponseDto{
        private GradeDto gradeDto;
        private int daysFromCreated;
        private MyPageProfile profile;
    }

    @Builder
    @Data
    public static class MyPageGradeResponseDto{
        private GradeDto gradeDto;
        private int nextLevelToGetItems;
        private List<ItemDtoV2> itemDtoList;
        private Map<String,String> levelGroups;
    }
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MyPageProfileNameValidateDto{
        private String name;
    }

    @Data
    @Builder
    public static class MyPageProfile{
        private String name;
        private String profileUrl;
    }

    @Data
    @Builder
    @AllArgsConstructor
    public static class MyPageAlarm{
        private Boolean todoAlarm;
    }
}
