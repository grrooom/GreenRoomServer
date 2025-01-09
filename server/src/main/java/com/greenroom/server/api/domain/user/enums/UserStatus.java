package com.greenroom.server.api.domain.user.enums;

import lombok.Getter;

@Getter
public enum UserStatus {

    IN_ACTION("활동중인 계정"),
    DELETE_PENDING("회원 삭제 대기중");

    @Getter
    private String description;

    UserStatus(String description) {
        this.description = description;
    }

}
