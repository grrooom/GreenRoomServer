package com.greenroom.server.api.security.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
public enum VerificationStatus {

    PENDING("인증 대기중"),
    VERIFIED("인증됨");

    private final String description;

    VerificationStatus(String description){
        this.description =description;
    }
}
