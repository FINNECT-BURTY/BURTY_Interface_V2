package com.berty.core.jenkins.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.IOException;

/**
 * secretKey.json 파일을 읽어서 설정을 로드하는 클래스
 */
@Slf4j
@Configuration
public class SecretKeyLoader {

    @Autowired
    private JenkinsProperties jenkinsProperties;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void loadSecretKeys() {
        try {
            File secretFile = new File("secretKey.json");

            if (!secretFile.exists()) {
                log.warn("secretKey.json file not found. Using default configuration from application.properties");
                return;
            }

            JsonNode root = objectMapper.readTree(secretFile);

            if (root.has("jenkins")) {
                JsonNode jenkins = root.get("jenkins");

                if (jenkins.has("url")) {
                    jenkinsProperties.setUrl(jenkins.get("url").asText());
                }
                if (jenkins.has("username")) {
                    jenkinsProperties.setUsername(jenkins.get("username").asText());
                }
                if (jenkins.has("token")) {
                    jenkinsProperties.setToken(jenkins.get("token").asText());
                }
                if (jenkins.has("connectionTimeout")) {
                    jenkinsProperties.setConnectionTimeout(jenkins.get("connectionTimeout").asInt());
                }
                if (jenkins.has("readTimeout")) {
                    jenkinsProperties.setReadTimeout(jenkins.get("readTimeout").asInt());
                }

                log.info("Successfully loaded Jenkins configuration from secretKey.json");
            }

        } catch (IOException e) {
            log.error("Failed to load secretKey.json. Using default configuration.", e);
        }
    }
}
