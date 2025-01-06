package com.greenroom.server.api.domain.alram.repository;

import com.greenroom.server.api.domain.alram.entity.Alarm;
import com.greenroom.server.api.domain.user.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AlarmRepository extends JpaRepository<Alarm,Long> {

    @EntityGraph(attributePaths = "user")
    Optional<Alarm> findByUser(User user);
}
