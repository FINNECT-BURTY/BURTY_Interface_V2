package com.burty.core.jenkins.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Jenkins 연결 설정을 관리하는 Properties 클래스
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "jenkins")
public class JenkinsProperties {

    // Jenkins 서버 URL
    private String url;

    // Jenkins 사용자명
    private String username;

    // Jenkins API 토큰
    private String token;

    // 연결 타임아웃 (밀리초)
    private Integer connectionTimeout = 30000;

    // 읽기 타임아웃 (밀리초)
    private Integer readTimeout = 60000;
}
