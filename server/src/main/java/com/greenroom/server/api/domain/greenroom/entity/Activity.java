package com.greenroom.server.api.domain.greenroom.entity;

import com.greenroom.server.api.domain.common.BaseTime;
import com.greenroom.server.api.domain.greenroom.enums.ActivityName;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(name = "activity")
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Activity extends BaseTime {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long activityId;

    @Enumerated(EnumType.STRING)
    private ActivityName name;

    @Builder
    public Activity(Long activityId, ActivityName name) {
        this.activityId = activityId;
        this.name = name;
    }
}
