package com.greenroom.server.api.domain.user.repository;

import com.greenroom.server.api.domain.user.entity.CheckIn;
import com.greenroom.server.api.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CheckInRepository extends JpaRepository<CheckIn,Long> {

    Optional<CheckIn> findByUser(User user);

    @Query("delete from CheckIn c where c.user =:user")
    @Modifying
    void deleteByUser(@Param("user")User user);
}
