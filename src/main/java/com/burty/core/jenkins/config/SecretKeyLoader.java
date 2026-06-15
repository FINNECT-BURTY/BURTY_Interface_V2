/**
 *
 *
 * <pre>
 * <b>Description  : 설정 (SecretKeyLoader)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.core.jenkins.config
 * </pre>
 *
 * @author : RosieOh
 * @version : 1.0
 * @since
 *     <pre>
 * Modification Information
 *    수정일              수정자                수정내용
 * ---------------   ---------------   ----------------------------
 *  2026.06.15        RosieOh     최초생성
 *        </pre>
 */
package com.burty.core.jenkins.config;

import com.burty.core.constant.LogMessages;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

/** secretKey.json 파일을 읽어서 설정을 로드하는 클래스 */
@Slf4j
@Configuration
public class SecretKeyLoader {

  @Autowired private JenkinsProperties jenkinsProperties;

  private final ObjectMapper objectMapper = new ObjectMapper();

  @PostConstruct
  public void loadSecretKeys() {
    try {
      File secretFile = new File("secretKey.json");

      if (!secretFile.exists()) {
        log.warn(
            "secretKey.json file not found. Using default configuration from application.properties");
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

        log.info(LogMessages.Jenkins.SECRET_LOADED);
      }

    } catch (IOException e) {
      log.error("Failed to load secretKey.json. Using default configuration.", e);
    }
  }
}
