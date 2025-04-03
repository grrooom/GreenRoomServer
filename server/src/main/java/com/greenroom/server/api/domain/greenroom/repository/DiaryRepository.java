package com.greenroom.server.api.domain.greenroom.repository;

import com.greenroom.server.api.domain.greenroom.dto.in.DiaryImageSimpleDto;
import com.greenroom.server.api.domain.greenroom.entity.Diary;
import com.greenroom.server.api.domain.greenroom.entity.GreenRoom;
import com.greenroom.server.api.domain.greenroom.entity.TodoLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Repository
public interface DiaryRepository extends JpaRepository<Diary,Long> {

    @Modifying
    @Query("delete from Diary d where d.greenRoom.greenroomId in (:ids)")
    void deleteAllByGreenRoom(@Param("ids")Collection<Long> greenRoom);

    @Query("select new com.greenroom.server.api.domain.greenroom.dto.in.DiaryImageSimpleDto(d.diaryId,d.diaryPictureUrl) from Diary d where d.greenRoom.greenroomId in (:ids)")
    List<DiaryImageSimpleDto> findAllByGreenRoomIn(@Param(("ids"))Collection<Long> greenRoom);

    List<Diary> findAllByGreenRoomGreenroomId(Long greenroomId);

    void deleteAllByGreenRoom(GreenRoom greenRoom);

    @Query("select d from Diary d where FUNCTION('DATE', d.createDate)=:date And d.greenRoom.greenroomId in :greenRooms ")
    List<Diary> findByCreateDateAndGreenRoomIn(@Param("date")LocalDate date, @Param("greenRooms") List<Long> greenRoom);

}
