package com.greenroom.server.api.security.entity;


import com.greenroom.server.api.domain.common.entity.BaseTime;
import com.greenroom.server.api.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Entity
@Builder
@RequiredArgsConstructor
@AllArgsConstructor
@Table(name = "refresh_token")
public class RefreshToken extends BaseTime {

    @Id@GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long refreshTokenId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="user_id",nullable = false)
    private User user;

    private String refreshToken;

    public void updateRefreshToken(String refreshToken){
        this.refreshToken = refreshToken;
    }

    public static RefreshToken createRefreshToken(User user, String refreshToken){
        return RefreshToken.builder().refreshToken(refreshToken).user(user).build();
    }
}
