package com.greenroom.server.api.domain.user.dto;

import com.greenroom.server.api.domain.user.entity.User;
import lombok.*;

@Data
@Getter
@AllArgsConstructor
@RequiredArgsConstructor
@Builder
public class UserInfoResponseDto {

    private String userName;
    private String email;
    private Long userDurationWithGreenroom;
    private Integer level;
    private String levelName;
    private String profileImgUrl;
    private Integer seedsToNextLevel;

    public static UserInfoResponseDto from(User user, Integer seedsToNextLevel, Long userDurationWithGreenroom,String imagePrefix){

        String imageUrl = user.getProfileUrl();
        imageUrl = imageUrl==null? null: imagePrefix+imageUrl;


        return UserInfoResponseDto
                .builder()
                .userName(user.getName())
                .email(user.getEmail())
                .level(user.getGrade().getLevel())
                .levelName(user.getGrade().getGradeName())
                .profileImgUrl(imageUrl)
                .seedsToNextLevel(seedsToNextLevel)
                .userDurationWithGreenroom(userDurationWithGreenroom)
                .build();
    }
}
