package com.greenroom.server.api.domain.greenroom.repository;

import com.greenroom.server.api.domain.greenroom.entity.GreenRoom;
import com.greenroom.server.api.domain.greenroom.entity.Todo;
import com.greenroom.server.api.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;

@Repository
public interface TodoRepository extends JpaRepository<Todo, Long> {

    @Modifying
    @Query("delete from Todo t where t.greenRoom.greenroomId in (:ids)")
    void deleteAllByGreenRoom(@Param("ids")Collection<Long> greenRoom);



}
