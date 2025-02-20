package com.greenroom.server.api.domain.admin;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.greenroom.server.api.domain.greenroom.service.PlantService;
import com.greenroom.server.api.domain.user.entity.User;
import com.greenroom.server.api.domain.user.service.UserService;
import com.greenroom.server.api.security.service.CustomUserDetailService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    @PersistenceContext
    private final EntityManager entityManager;
    private final CustomUserDetailService customUserDetailService;
    private final UserService userService;
    private final PlantService plantService;

    private final List<String> notDeletedTableList = List.of("plant","grade","item","user_exit_reason","activity");

    public List<String> getTables(){
        List<String> result =  entityManager.createNativeQuery("SHOW TABLES").getResultList();
        result.removeAll(notDeletedTableList);
        return  result;
    }

    @Transactional
    public void deleteAllData(){
        List<String> tables = getTables();

        entityManager.createNativeQuery(String.format("SET FOREIGN_KEY_CHECKS=%d", 0)).executeUpdate();
        for (String tableName : tables) {
            entityManager.createNativeQuery(String.format("TRUNCATE TABLE %s", tableName)).executeUpdate();
        }
        entityManager.createNativeQuery(String.format("SET FOREIGN_KEY_CHECKS=%d", 1)).executeUpdate();

    }

    public void deleteSpecificUser(String email){
        User user = customUserDetailService.findUserByEmail(email);
        userService.deleteAllWithUser(user);
    }


    public void insertPlantDataIntoDB() throws JsonProcessingException {
        plantService.insertPlantDataIntoDB();
    }

    //식물 전체 데이터 삭제

    public void deleteAllPlantData() {
        plantService.deleteData();
    }



    public void uploadPlantImageToS3() throws IOException {
        plantService.uploadPlantImageToS3();
    }


    public void updatePlantDocumentWithPlant(){
        plantService.updatePlantDocumentWithPlant();
    }



}
