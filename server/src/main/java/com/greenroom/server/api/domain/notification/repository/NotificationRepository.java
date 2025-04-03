package com.greenroom.server.api.domain.notification.repository;

import com.greenroom.server.api.domain.notification.entity.Notification;
import com.greenroom.server.api.domain.user.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification,Long> {

    Optional<Notification> findByUser(User user);

    @Query("delete from Notification n where n.user =:user")
    @Modifying
    void deleteByUser(@Param("user")User user);

}
