package com.greenroom.server.api.domain.greenroom.repository;

import com.greenroom.server.api.domain.greenroom.entity.Adornment;
import com.greenroom.server.api.domain.greenroom.entity.GreenRoom;
import com.greenroom.server.api.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;

@Repository
public interface AdornmentRepository extends JpaRepository<Adornment, Long> {

    @Modifying
    @Query("delete from Adornment a where a.greenRoom.greenroomId in (:ids)")
    void deleteAllByGreenRoom(@Param("ids")Collection<Long> greenRoom);



}
