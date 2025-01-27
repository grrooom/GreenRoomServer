package com.greenroom.server.api.domain.admin;


import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    @PersistenceContext
    private final EntityManager entityManager;

    public List<String> getTables(){
        return entityManager.createNativeQuery("SHOW TABLES").getResultList();
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

}
