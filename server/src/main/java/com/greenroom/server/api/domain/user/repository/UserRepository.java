package com.greenroom.server.api.domain.user.repository;

import com.greenroom.server.api.domain.user.entity.User;
import com.greenroom.server.api.domain.user.enums.UserStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    @EntityGraph(attributePaths = {"grade"})
    Optional<User> findByEmail(String email);

    int deleteAllByUserStatus(UserStatus status);
}
