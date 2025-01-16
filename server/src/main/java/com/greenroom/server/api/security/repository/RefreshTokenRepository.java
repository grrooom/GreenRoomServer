package com.greenroom.server.api.security.repository;

import com.greenroom.server.api.security.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken,String> {

    Optional<RefreshToken> findRefreshTokenByEmail(String email);

    void deleteRefreshTokenByEmail(String email);
}
