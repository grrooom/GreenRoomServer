package com.greenroom.server.api.domain.greenroom.repository;

import com.greenroom.server.api.domain.greenroom.entity.GreenRoom;
import com.greenroom.server.api.domain.greenroom.entity.TodoLog;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface TodoLogRepository extends JpaRepository<TodoLog,Long> {
    @Query("delete from TodoLog tl where tl.greenRoom.greenroomId in (:ids)")
    @Modifying
    void deleteAllByGreenRoom(@Param("ids")Collection<Long> todo_greenRoom);

    @Query("delete from TodoLog tl where tl.greenRoom.greenroomId =(:id)")
    @Modifying
    void deleteAllByGreenRoomGreenroomId(@Param("id")Long greenroomId);

    @EntityGraph(attributePaths ={"activity"})
    @Query("select tl from TodoLog tl where FUNCTION('DATE', tl.createDate) =:date And tl.greenRoom.greenroomId in (:greenRooms) ")
    List<TodoLog> findByCreateDateAndGreenRoomIn( @Param("date") LocalDate date, @Param("greenRooms") List<Long> greenRooms);
}
