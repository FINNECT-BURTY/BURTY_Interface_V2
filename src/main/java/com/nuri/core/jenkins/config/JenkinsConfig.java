package com.nuri.core.jenkins.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.Base64;

// Jenkins 클라이언트 설정을 담당하는 Configuration 클래스
@Slf4j
@Configuration
@RequiredArgsConstructor
public class JenkinsConfig {

    private final JenkinsProperties jenkinsProperties;

    // Jenkins API 호출을 위한 WebClient Bean 생성
    @Bean
    public WebClient jenkinsWebClient() {

        // Basic Authentication 헤더 생성
        String auth = jenkinsProperties.getUsername() + ":" + jenkinsProperties.getToken();
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());

        // HttpClient 설정 (타임아웃)
        HttpClient httpClient = HttpClient.create()
            .responseTimeout(Duration.ofMillis(jenkinsProperties.getReadTimeout()));

        return WebClient.builder()
            .baseUrl(jenkinsProperties.getUrl())
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + encodedAuth)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .build();
    }
}
