package com.greenroom.server.api.domain.user.repository;

import com.greenroom.server.api.domain.user.entity.UserExitReason;
import com.greenroom.server.api.domain.user.enums.UserExitReasonType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserExitReasonRepository extends JpaRepository<UserExitReason, Long> {

    List<UserExitReason> findAllByReasonType(UserExitReasonType userExitReasonType);
}
