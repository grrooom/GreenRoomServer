package com.greenroom.server.api.domain.user.repository;

import com.greenroom.server.api.domain.user.entity.User;
import com.greenroom.server.api.domain.user.enums.UserStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    @EntityGraph(attributePaths = {"grade"})
    Optional<User> findByEmail(String email);

    Optional<User> findUserByEmailAndUserStatus(String email, UserStatus userStatus);

    List<User> findAllByUserStatusAndDeleteDateBefore(UserStatus userStatus, LocalDateTime updateDate);
    Boolean existsByEmail(String email);
}
