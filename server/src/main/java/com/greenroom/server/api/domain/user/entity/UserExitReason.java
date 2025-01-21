package com.greenroom.server.api.domain.user.entity;

import com.greenroom.server.api.domain.user.enums.UserExitReasonType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import lombok.*;
import jakarta.persistence.*;

@Entity
@Getter
@RequiredArgsConstructor
@AllArgsConstructor
@Builder
public class UserExitReason {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userExitReasonId;

    private String reason;

    private Long count;

    @Enumerated(EnumType.STRING)
    private UserExitReasonType reasonType;

    public void updateCount(){
        this.count +=1;
    }

    public static UserExitReason createCustomReason(String reason){
        return UserExitReason.builder().reason(reason).count(1L).reasonType(UserExitReasonType.CUSTOM).build();
    }

}
