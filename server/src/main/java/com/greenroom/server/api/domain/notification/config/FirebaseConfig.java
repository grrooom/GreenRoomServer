package com.greenroom.server.api.domain.notification.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@Configuration
public class FirebaseConfig {

    @PostConstruct
    public FirebaseApp initializeFcm() throws IOException {

        //테스트 클래스 로드시 applicationContext가 여러번 로드됨 -> applicationContext 외부에서 바라볼 때 여러개의 bean객체가 생성됨 -> firebase sdk는 전역적으로 하나의 singleton 객체를 강제함. -> 에러 발생
        if (!FirebaseApp.getApps().isEmpty()) { //이미 존재하면 다시 생성하지 않음
            return FirebaseApp.getInstance();
        }

        InputStream serviceAccount = getClass().getResourceAsStream("/fcm-admin.json");


        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .build();
        return FirebaseApp.initializeApp(options);
    }
}