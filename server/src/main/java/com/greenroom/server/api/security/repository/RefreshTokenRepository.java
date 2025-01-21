package com.greenroom.server.api.security.repository;

import com.greenroom.server.api.domain.user.entity.User;
import com.greenroom.server.api.security.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken,String> {


    void deleteRefreshTokenByUser(User user);

    Boolean existsByUser(User user);

    Optional<RefreshToken> findRefreshTokenByUser(User user);


}
