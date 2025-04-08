package com.greenroom.server.api.domain.greenroom.repository;

import com.greenroom.server.api.domain.greenroom.entity.GreenRoom;
import com.greenroom.server.api.domain.greenroom.entity.Todo;
import com.greenroom.server.api.domain.greenroom.entity.TodoLog;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Repository
public interface TodoRepository extends JpaRepository<Todo, Long> {

    @Modifying
    @Query("delete from Todo t where t.greenRoom.greenroomId in (:ids)")
    void deleteAllByGreenRoom(@Param("ids")Collection<Long> greenRoom);

    @EntityGraph(attributePaths = {"activity"})
    List<Todo> findAllByGreenRoomAndUseYn(GreenRoom greenRoom, Boolean useYn);

    @EntityGraph(attributePaths = {"activity"})
    List<Todo> findAllByGreenRoomAndUseYnAndNextTodoDateIsNotNull(GreenRoom greenRoom, Boolean useYn);

    @Query("select t from Todo t where t.greenRoom.greenroomId = :greenroomId and t.activity.activityId in (:activityIds)")
    List<Todo> findAllByGreenRoomAndActivity(@Param("greenroomId")Long greenroomId,@Param("activityIds") List<Long> activityIds );

    @EntityGraph(attributePaths ={"activity"})
    List<Todo> findAllByGreenRoom(GreenRoom greenRoom);

    @EntityGraph(attributePaths ={"activity"})
    @Query("select t from Todo t where FUNCTION('DATE', t.nextTodoDate)<=:date And t.greenRoom.greenroomId in :greenRooms ")
    List<Todo> findByNextTodoDateAndGreenRoomIn(@Param("date") LocalDate nextTodoDate,@Param("greenRooms")List<Long> greenRoomList);
    @EntityGraph(attributePaths ={"activity"})
    @Query("select t from Todo t where FUNCTION('DATE', t.nextTodoDate) =:date And t.greenRoom.greenroomId in :greenRooms ")
    List<Todo> findByNextTodoDateAndGreenRoomInFuture(@Param("date") LocalDate date,@Param("greenRooms")List<Long> greenRoomList);
}
