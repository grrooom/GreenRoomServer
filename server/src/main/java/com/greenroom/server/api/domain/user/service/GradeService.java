package com.greenroom.server.api.domain.user.service;

import com.greenroom.server.api.domain.greenroom.dto.out.PointAndLevelUpResponseDto;
import com.greenroom.server.api.domain.user.entity.Grade;
import com.greenroom.server.api.domain.user.entity.User;
import com.greenroom.server.api.domain.user.repository.GradeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.logging.Level;

@Service
@RequiredArgsConstructor
public class GradeService {

    private final GradeRepository gradeRepository;

    public Grade getGradeByTotalSeeds(Integer totalSeeds){
        return gradeRepository.findGradeByTotalSeeds(totalSeeds).orElse(null);
    }


    public PointAndLevelUpResponseDto.LevelUpStatus updateUserGrade(User user){

        Grade oldGrade = user.getGrade();

        Grade newGrade = getGradeByTotalSeeds(user.getTotalSeed()); //증가된 포인트로 레벨 조회

        PointAndLevelUpResponseDto.LevelUpStatus levelUpStatus =  new PointAndLevelUpResponseDto.LevelUpStatus(oldGrade!=newGrade, newGrade.getLevel());

        if(newGrade!=oldGrade){user.updateGrade(newGrade);}

        return levelUpStatus;
    }

}
