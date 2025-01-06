package com.greenroom.server.api.domain.board.entity;

import com.greenroom.server.api.domain.common.BaseTime;
import com.greenroom.server.api.domain.greenroom.entity.Plant;
import com.greenroom.server.api.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(name = "suggestion")
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Suggestion extends BaseTime {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long suggestionId;

    private Boolean isRegistered;

    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    public Suggestion(String content, User user, Plant plant) {
        this.isRegistered = Boolean.FALSE;
        this.content = content;
        this.user = user;
    }
}
