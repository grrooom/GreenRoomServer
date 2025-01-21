package com.greenroom.server.api.config;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Component;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

import java.util.List;

@Component
@Slf4j
public class TestDatabaseInitializer  {

    private static boolean initialized = false;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    @Modifying
    public void resetDatabase()  {

        //test 시 table 생성 & data 삽입
        //모든 test class에 대하여 한번만 실행되도록 함.

        if (!initialized) {
            entityManager.createNativeQuery("RUNSCRIPT FROM 'classpath:schema-test.sql'").executeUpdate();
            entityManager.createNativeQuery("RUNSCRIPT FROM 'classpath:data-test.sql'").executeUpdate();
            initialized = true;
        }
    }

}

