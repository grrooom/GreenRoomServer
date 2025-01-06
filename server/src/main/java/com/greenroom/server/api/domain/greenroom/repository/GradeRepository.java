package com.greenroom.server.api.domain.greenroom.repository;

import com.greenroom.server.api.domain.greenroom.entity.Grade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GradeRepository extends JpaRepository<Grade,Long> {

    Optional<Grade> findDistinctFirstByRequiredSeedLessThanEqualOrderByRequiredSeedDesc(int requiredSeed);

    Optional<Grade> findFirstByLevelGreaterThanOrderByLevelAsc(int level);

    @Query("""
    select g From Grade g 
    join Item i on i.grade = g
    where g.level > :level
    order by g.level asc limit 1
    """)
    Optional<Grade> findNextGradeHasItems(@Param("level") int level);

    Optional<Grade> findByLevel(int level);

}