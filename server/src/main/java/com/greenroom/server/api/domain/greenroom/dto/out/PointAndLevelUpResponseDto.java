package com.greenroom.server.api.domain.greenroom.dto.out;

import com.greenroom.server.api.domain.user.entity.User;

import java.util.List;

public record PointAndLevelUpResponseDto (

    Integer earnedPoints,
    LevelUpStatus levelUpStatus,
    List<LevelUpDetail> levelUpDetails){

    public record LevelUpDetail(String source, Integer points){}

    public record LevelUpStatus(Boolean isLevelUp, Integer currentLevel){
        public  static LevelUpStatus of(User user){
            return new LevelUpStatus(false,user.getGrade().getLevel());
        }
    }

    public static PointAndLevelUpResponseDto of(User user, Integer point, LevelUpStatus levelUpStatus){
        return new PointAndLevelUpResponseDto(point,levelUpStatus,null);
    }

    public static PointAndLevelUpResponseDto ofFirstGreenroomRegistration(User user, Integer point, LevelUpStatus levelUpStatus){
        return new PointAndLevelUpResponseDto(point,(levelUpStatus),
                List.of(new LevelUpDetail("첫 방문",1),
                        new LevelUpDetail("식물 등록",1)));
    }
}
