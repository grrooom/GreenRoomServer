package com.greenroom.server.api.domain.user.dto;

import com.greenroom.server.api.domain.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Getter
@AllArgsConstructor
public class UserBaseInfoDto {
    private String userName;
    private Long userID;
    private String imgUrl;
    private Long period;

    public static UserBaseInfoDto from(User user){

        LocalDateTime today = LocalDateTime.now();
        Long period = ChronoUnit.DAYS.between(user.getCreateDate(),today) +1;

        return new UserBaseInfoDto( user.getName(),user.getUserId(),user.getProfileUrl(),period);
    }
}



