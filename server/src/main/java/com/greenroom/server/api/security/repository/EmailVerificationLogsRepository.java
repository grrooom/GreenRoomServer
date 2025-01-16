package com.greenroom.server.api.security.repository;

import com.greenroom.server.api.security.entity.EmailVerificationLogs;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailVerificationLogsRepository extends JpaRepository<EmailVerificationLogs,Long> {

    Optional<EmailVerificationLogs> findByEmail(String email);

    Optional<EmailVerificationLogs> findByVerificationToken(String token);

    Boolean existsByEmail(String email);

    void deleteByEmail(String email);
}
