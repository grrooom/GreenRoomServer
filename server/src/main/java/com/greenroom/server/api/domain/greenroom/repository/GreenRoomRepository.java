package com.greenroom.server.api.domain.greenroom.repository;

import com.greenroom.server.api.domain.greenroom.dto.in.GreenroomImageSimpleDto;
import com.greenroom.server.api.domain.greenroom.entity.GreenRoom;
import com.greenroom.server.api.domain.greenroom.enums.GreenRoomStatus;
import com.greenroom.server.api.domain.user.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface GreenRoomRepository extends JpaRepository<GreenRoom,Long> {

    List<GreenRoom> findAllByUser(User user);

    @Modifying
    @Query("select new com.greenroom.server.api.domain.greenroom.dto.in.GreenroomImageSimpleDto(g.greenroomId,g.pictureUrl) from GreenRoom g where g.user = :user")
    List<GreenroomImageSimpleDto> findAllGreenRoomImageByUser(@Param("user")User user);


    @Modifying
    @Query("delete from GreenRoom g where g.greenroomId in (:ids)")
    void deleteAllByGreenroomId(@Param("ids")Collection<Long> greenroomId);


    @EntityGraph(attributePaths = {"plant"})
    List<GreenRoom> findGreenRoomByUserAndGreenroomStatus(User user, GreenRoomStatus greenRoomStatus);

    @EntityGraph(attributePaths = {"plant"})
    List<GreenRoom> findGreenRoomByUser(User user);

    @EntityGraph(attributePaths = {"plant"})
    Optional<GreenRoom> findByGreenroomIdAndGreenroomStatus(Long greenroomId, GreenRoomStatus greenRoomStatus);
}
