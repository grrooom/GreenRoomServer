package com.greenroom.server.api.domain.greenroom.repository;

import com.fasterxml.jackson.annotation.OptBoolean;
import com.greenroom.server.api.domain.greenroom.entity.Activity;
import org.checkerframework.checker.units.qual.A;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ActivityRepository extends JpaRepository<Activity,Long> {

    Optional<Activity> findActivityByActivityName(String activityName);

}
