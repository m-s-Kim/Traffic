package com.commerce.backend.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;

@Configuration
public class FirebaseConfig {

//    @Bean
//    public FirebaseApp initializeFirebase() throws IOException {
//        FileInputStream serviceAccount =
//                new FileInputStream("path/to/your/firebase-service-account.json");  // 서비스 계정 키 파일 경로
//
//        FirebaseOptions options = FirebaseOptions.builder()
//                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
//                .build();
//
//        if (FirebaseApp.getApps().isEmpty()) {
//            return FirebaseApp.initializeApp(options);
//        }
//        return FirebaseApp.getInstance();
//    }
}