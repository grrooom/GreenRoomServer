package com.greenroom.server.api.domain.greenroom.entity;

import com.greenroom.server.api.domain.common.entity.BaseTime;
import jakarta.persistence.*;
import lombok.*;

@Table(name = "grade")
@Entity
@Getter
@ToString
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Grade extends BaseTime {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long gradeId;

    private String description;

    private String gradeImageUrl;

    private int requiredSeed;

    private int level;

    @Builder
    public Grade(String description,String gradeImageUrl,int requiredSeed,int level){
        this.description =description;
        this.gradeImageUrl = gradeImageUrl;
        this.requiredSeed = requiredSeed;
        this.level = level;
    }
}
