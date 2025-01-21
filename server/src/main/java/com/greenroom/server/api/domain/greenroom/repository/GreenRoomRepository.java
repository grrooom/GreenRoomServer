package com.greenroom.server.api.domain.greenroom.repository;

import com.greenroom.server.api.domain.greenroom.entity.GreenRoom;
import com.greenroom.server.api.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface GreenRoomRepository extends JpaRepository<GreenRoom,Long> {

    List<GreenRoom> findAllByUser(User user);

    @Modifying
    @Query("select g.greenroomId from GreenRoom g where g.user = :user")
    List<Long> findAllGreenRoomIdByUser(@Param("user")User user);


    @Modifying
    @Query("delete from GreenRoom g where g.greenroomId in (:ids)")
    void deleteAllByGreenroomId(@Param("ids")Collection<Long> greenroomId);


}
