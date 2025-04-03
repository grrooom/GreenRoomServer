package com.greenroom.server.api.security.repository;

import com.greenroom.server.api.domain.user.entity.User;
import com.greenroom.server.api.security.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken,String> {


    void deleteRefreshTokenByUser(User user);

    Boolean existsByUser(User user);

    Optional<RefreshToken> findRefreshTokenByUser(User user);

    @Query("delete from RefreshToken r where r.user =:user")
    @Modifying
    void deleteByUser(@Param("user")User user);

}
