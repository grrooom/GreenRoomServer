package com.greenroom.server.api.security.entity;


import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.*;
import org.springframework.data.redis.core.RedisHash;

@Getter
@Entity
@Builder
@RequiredArgsConstructor
@AllArgsConstructor
public class RefreshToken {

    @Id@GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long refreshTokenId;
    private String email;
    private String refreshToken;

    public void updateRefreshToken(String refreshToken){
        this.refreshToken = refreshToken;
    }
}
