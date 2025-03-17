package com.greenroom.server.api.domain.greenroom.repository;

import com.greenroom.server.api.domain.greenroom.entity.TodoLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;

@Repository
public interface TodoLogRepository extends JpaRepository<TodoLog,Long> {
    @Query("delete from TodoLog tl where tl.greenRoom.greenroomId in (:ids)")
    @Modifying
    void deleteAllByGreenRoom(@Param("ids")Collection<Long> todo_greenRoom);

    @Query("delete from TodoLog tl where tl.greenRoom.greenroomId =(:id)")
    @Modifying
    void deleteAllByGreenRoomGreenroomId(@Param("id")Long greenroomId);
}
