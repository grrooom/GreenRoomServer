package com.greenroom.server.api.security.entity;


import com.greenroom.server.api.domain.common.BaseTime;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.redis.core.RedisHash;

@Getter
@Entity
@Builder
@RequiredArgsConstructor
@AllArgsConstructor
@Table(name = "refresh_token")
public class RefreshToken extends BaseTime {

    @Id@GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long refreshTokenId;
    private String email;
    private String refreshToken;

    public void updateRefreshToken(String refreshToken){
        this.refreshToken = refreshToken;
    }
}
