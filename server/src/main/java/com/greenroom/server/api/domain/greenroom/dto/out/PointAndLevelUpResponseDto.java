package com.greenroom.server.api.domain.greenroom.dto.out;

import com.greenroom.server.api.domain.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class PointAndLevelUpResponseDto {

    private Integer earnedPoints;

    private LevelUpStatus levelUpStatus;

    private List<LevelUpDetail> levelUpDetails;

    public record LevelUpDetail(String source, Integer points){}

    public record LevelUpStatus(Boolean isLevelUp, Integer currentLevel){
        public  static LevelUpStatus of(User user){
            return new LevelUpStatus(false,user.getGrade().getLevel());
        }
    }

    public static PointAndLevelUpResponseDto of(User user, Integer point, LevelUpStatus levelUpStatus){
        return PointAndLevelUpResponseDto.builder()
                .earnedPoints(point)
                .levelUpStatus(levelUpStatus)
                .levelUpDetails(null)
                .build();
    }

    public static PointAndLevelUpResponseDto ofFirstGreenroomRegistration(User user, Integer point, LevelUpStatus levelUpStatus){
        return PointAndLevelUpResponseDto.builder()
                .earnedPoints(point)
                .levelUpStatus(levelUpStatus)
                .levelUpDetails(List.of(
                                new LevelUpDetail("첫 방문",1),
                                new LevelUpDetail("식물 등록",1)))
                .build();
    }
}
