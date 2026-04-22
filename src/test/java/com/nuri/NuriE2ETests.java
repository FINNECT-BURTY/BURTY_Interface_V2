package com.nuri;

import com.nuri.domain.entity.UserEntity;
import com.nuri.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class NuriE2ETests {
    @LocalServerPort
    private int port;
    @Autowired
    private UserRepository userRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    private String userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID().toString();
        UserEntity user = new UserEntity();
        user.setUserId(UUID.fromString(userId));
        user.setCiHash("c".repeat(64));
        user.setCiEncrypted("ci".getBytes());
        user.setPhoneHash("d".repeat(64));
        user.setPhoneEncrypted("phone".getBytes());
        user.setStatus(UserEntity.UserStatus.ACTIVE);
        user.setFailedLoginCount(0);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    @Test
    void authToConsultToFamilyAlertFlow() throws Exception {
        String base = "http://localhost:" + port;
        ResponseEntity<String> tokenEntity = restTemplate.postForEntity(
                base + "/api/nuri/auth/token",
                new HttpEntity<>("{\"userId\":\"" + userId + "\"}", jsonHeaders()),
                String.class
        );
        String tokenResp = tokenEntity.getBody();
        String token = extractToken(tokenResp);

        HttpHeaders authHeaders = jsonHeaders();
        authHeaders.set("Authorization", "Bearer " + token);
        ResponseEntity<String> consult = restTemplate.postForEntity(
                base + "/api/nuri/consult",
                new HttpEntity<>("{\"userId\":\"" + userId + "\",\"question\":\"이번달 괜찮아?\"}", authHeaders),
                String.class
        );
        if (!consult.getStatusCode().is2xxSuccessful()) throw new RuntimeException("consult failed");

        HttpHeaders getHeaders = new HttpHeaders();
        getHeaders.set("Authorization", "Bearer " + token);
        ResponseEntity<String> alerts = restTemplate.exchange(
                base + "/api/nuri/family-alerts?userId=" + userId,
                org.springframework.http.HttpMethod.GET,
                new HttpEntity<>(getHeaders),
                String.class
        );
        if (!alerts.getStatusCode().is2xxSuccessful()) throw new RuntimeException("alerts failed");
    }

    private HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private String extractToken(String json) {
        if (json == null) return "";
        String key = "\"accessToken\":\"";
        int idx = json.indexOf(key);
        if (idx < 0) return "";
        int start = idx + key.length();
        int end = json.indexOf("\"", start);
        return end > start ? json.substring(start, end) : "";
    }
}
