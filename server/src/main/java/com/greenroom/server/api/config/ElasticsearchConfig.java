//package com.greenroom.server.api.config;
//
//import lombok.RequiredArgsConstructor;
//import org.apache.hc.core5.ssl.SSLContextBuilder;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.boot.context.properties.ConfigurationProperties;
//import org.springframework.boot.context.properties.EnableConfigurationProperties;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.context.annotation.Profile;
//import org.springframework.data.elasticsearch.client.ClientConfiguration;
//import org.springframework.data.elasticsearch.client.elc.ReactiveElasticsearchConfiguration;
//
//import javax.net.ssl.SSLContext;
//import javax.net.ssl.TrustManagerFactory;
//import java.io.*;
//import java.nio.file.Paths;
//import java.security.KeyManagementException;
//import java.security.KeyStore;
//import java.security.KeyStoreException;
//import java.security.NoSuchAlgorithmException;
//import java.security.cert.CertificateException;
//import java.security.cert.CertificateFactory;
//import java.security.cert.X509Certificate;
//
//@Profile({"dev"})
//@Configuration
//@RequiredArgsConstructor
//public class ElasticsearchConfig extends ReactiveElasticsearchConfiguration {
//
//    @Value("${spring.elasticsearch.username}")
//    private String username;
//
//    @Value("${spring.elasticsearch.password}")
//    private String password;
//
//    @Value("${spring.elasticsearch.host}")
//    private String host;
//
//    @Value("${spring.elasticsearch.ssl}")
//    private String sslFile;
//
//    @Override
//    public ClientConfiguration clientConfiguration() {
//        return ClientConfiguration.builder()
//                .connectedTo(host)
//                .usingSsl(getSslContext())
//                .withBasicAuth(username,password)
//                .build();
//    }
//    private SSLContext getSslContext() {
//        try {
//            // 인증서 파일 경로
//            File certFile = Paths.get(sslFile).toFile();
//
//            // 인증서 로드
//            CertificateFactory factory = CertificateFactory.getInstance("X.509");
//            X509Certificate certificate;
//            try (FileInputStream fis = new FileInputStream(certFile)) {
//                certificate = (X509Certificate) factory.generateCertificate(fis);
//            }
//
//            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
//            keyStore.load(null, null);
//            keyStore.setCertificateEntry("ca", certificate);
//
//            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
//            tmf.init(keyStore);
//
//            SSLContext sslContext = SSLContext.getInstance("TLS");
//            sslContext.init(null, tmf.getTrustManagers(), new java.security.SecureRandom());
//            return sslContext;
//
//
//        } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException | IOException |
//                 CertificateException e) {
//            throw new RuntimeException("SSL 설정 중 오류 발생", e);
//        }
//    }
//}
//
