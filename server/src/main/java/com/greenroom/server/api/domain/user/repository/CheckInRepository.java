package com.greenroom.server.api.domain.user.repository;

import com.greenroom.server.api.domain.user.entity.CheckIn;
import com.greenroom.server.api.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CheckInRepository extends JpaRepository<CheckIn,Long> {

    Optional<CheckIn> findByUser(User user);
}
