package com.greenroom.server.api.domain.greenroom.repository;

import com.greenroom.server.api.domain.greenroom.entity.GreenRoom;
import com.greenroom.server.api.domain.greenroom.entity.TodoLog;
import com.greenroom.server.api.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;

@Repository
public interface TodoLogRepository extends JpaRepository<TodoLog,Long> {

    @Query("delete from TodoLog tl where tl.todo.greenRoom.greenroomId in (:ids)")
    @Modifying
    void deleteAllByGreenRoom(@Param("ids")Collection<Long> todo_greenRoom);

}
