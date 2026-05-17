package com.burty;

import com.burty.domain.entity.UserEntity;
import com.burty.domain.repository.ConsentRecordRepository;
import com.burty.domain.repository.SocialAccountRepository;
import com.burty.domain.repository.UserRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BurtyE2ETests {
    @LocalServerPort
    private int port;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private SocialAccountRepository socialAccountRepository;
    @Autowired
    private ConsentRecordRepository consentRecordRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    private String userId;

    @BeforeEach
    void setUp() {
        String nonce = UUID.randomUUID().toString().replace("-", "");
        UserEntity user = new UserEntity();
        user.setCiHash(nonce + nonce);
        user.setCiEncrypted("ci".getBytes());
        user.setPhoneHash(nonce + "0".repeat(32));
        user.setPhoneEncrypted("phone".getBytes());
        user.setStatus(UserEntity.UserStatus.ACTIVE);
        user.setFailedLoginCount(0);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userId = userRepository.save(user).getUserId().toString();
    }

    @Test
    void socialLoginIssuesBurtyTokenAndPersistsAccount() {
        String base = "http://localhost:" + port;
        ResponseEntity<String> login = restTemplate.postForEntity(
                base + "/api/v1/auth/kakao/login",
                new HttpEntity<>("{\"code\":\"test-social-code\",\"state\":\"state-1\"}", jsonHeaders()),
                String.class
        );

        Assertions.assertTrue(login.getStatusCode().is2xxSuccessful());
        String body = login.getBody();
        Assertions.assertNotNull(body);
        Assertions.assertTrue(body.contains("\"provider\":\"KAKAO\""));
        Assertions.assertTrue(body.contains("\"accessToken\":\""));
        Assertions.assertTrue(body.contains("\"profileComplete\":"));
        Assertions.assertTrue(socialAccountRepository.count() > 0);
    }

    @Test
    void googleSocialLoginIssuesBurtyToken() {
        String base = "http://localhost:" + port;
        ResponseEntity<String> login = restTemplate.postForEntity(
                base + "/api/v1/auth/google/login",
                new HttpEntity<>("{\"code\":\"test-google-code\",\"state\":\"state-g\"}", jsonHeaders()),
                String.class
        );

        Assertions.assertTrue(login.getStatusCode().is2xxSuccessful());
        String body = login.getBody();
        Assertions.assertNotNull(body);
        Assertions.assertTrue(body.contains("\"provider\":\"GOOGLE\""));
        Assertions.assertTrue(body.contains("\"accessToken\":\""));
    }

    @Test
    void profileOnboardingAfterSocialLogin() {
        String base = "http://localhost:" + port;
        String code = "onboard-" + UUID.randomUUID();
        ResponseEntity<String> login = restTemplate.postForEntity(
                base + "/api/v1/auth/kakao/login",
                new HttpEntity<>("{\"code\":\"" + code + "\"}", jsonHeaders()),
                String.class
        );
        Assertions.assertTrue(login.getStatusCode().is2xxSuccessful());
        String loginBody = login.getBody();
        Assertions.assertNotNull(loginBody);
        Assertions.assertTrue(loginBody.contains("\"profileComplete\":false"));
        String token = extractToken(loginBody);

        String phone = "010" + String.format("%08d", Math.abs(UUID.randomUUID().getLeastSignificantBits()) % 100_000_000);
        String onboardJson = "{"
                + "\"phone\":\"" + phone + "\","
                + "\"name\":\"온보딩테스트\","
                + "\"birthDate\":\"1996-08-21\","
                + "\"termsAccepted\":true"
                + "}";

        HttpHeaders authHeaders = jsonHeaders();
        authHeaders.setBearerAuth(token);
        ResponseEntity<String> onboard = restTemplate.postForEntity(
                base + "/api/v1/onboarding/profile",
                new HttpEntity<>(onboardJson, authHeaders),
                String.class
        );
        Assertions.assertTrue(onboard.getStatusCode().is2xxSuccessful());
        String ob = onboard.getBody();
        Assertions.assertNotNull(ob);
        Assertions.assertTrue(ob.contains("\"alreadyRegistered\":false"));

        String socialUserId = extractUserId(loginBody);
        Assertions.assertFalse(socialUserId.isEmpty());
        Assertions.assertTrue(consentRecordRepository.findByUser_UserId(Long.parseLong(socialUserId)).size() >= 2);

        ResponseEntity<String> login2 = restTemplate.postForEntity(
                base + "/api/v1/auth/kakao/login",
                new HttpEntity<>("{\"code\":\"" + code + "\"}", jsonHeaders()),
                String.class
        );
        Assertions.assertTrue(login2.getStatusCode().is2xxSuccessful());
        Assertions.assertNotNull(login2.getBody());
        Assertions.assertTrue(login2.getBody().contains("\"profileComplete\":true"));
        Assertions.assertTrue(login2.getBody().contains("\"newUser\":false"));

        ResponseEntity<String> onboard2 = restTemplate.postForEntity(
                base + "/api/v1/onboarding/profile",
                new HttpEntity<>(onboardJson, authHeaders),
                String.class
        );
        Assertions.assertTrue(onboard2.getStatusCode().is2xxSuccessful());
        Assertions.assertNotNull(onboard2.getBody());
        Assertions.assertTrue(onboard2.getBody().contains("\"alreadyRegistered\":true"));
    }

    @Test
    void demoSessionSeedsCashflowScenarioAndReturnsToken() {
        String base = "http://localhost:" + port;
        ResponseEntity<String> demo = restTemplate.postForEntity(
                base + "/api/v1/auth/demo/session",
                new HttpEntity<>("{}", jsonHeaders()),
                String.class
        );

        Assertions.assertTrue(demo.getStatusCode().is2xxSuccessful());
        String body = demo.getBody();
        Assertions.assertNotNull(body);
        Assertions.assertTrue(body.contains("\"userId\":\"demo-user\""));
        Assertions.assertTrue(body.contains("\"accessToken\":\""));
        Assertions.assertTrue(body.contains("월말 적자 반복형"));
    }

    @Test
    void authToConsultToFamilyAlertFlow() throws Exception {
        String base = "http://localhost:" + port;
        ResponseEntity<String> tokenEntity = restTemplate.postForEntity(
                base + "/api/v1/auth/token",
                new HttpEntity<>("{\"userId\":\"" + userId + "\"}", jsonHeaders()),
                String.class
        );
        String tokenResp = tokenEntity.getBody();
        String token = extractToken(tokenResp);

        HttpHeaders authHeaders = jsonHeaders();
        authHeaders.set("Authorization", "Bearer " + token);
        ResponseEntity<String> consult = restTemplate.postForEntity(
                base + "/api/v1/consult",
                new HttpEntity<>("{\"userId\":\"" + userId + "\",\"question\":\"이번달 괜찮아?\"}", authHeaders),
                String.class
        );
        if (!consult.getStatusCode().is2xxSuccessful()) throw new RuntimeException("consult failed");

        HttpHeaders getHeaders = new HttpHeaders();
        getHeaders.set("Authorization", "Bearer " + token);
        ResponseEntity<String> alerts = restTemplate.exchange(
                base + "/api/v1/family-alerts?userId=" + userId,
                org.springframework.http.HttpMethod.GET,
                new HttpEntity<>(getHeaders),
                String.class
        );
        if (!alerts.getStatusCode().is2xxSuccessful()) throw new RuntimeException("alerts failed");
    }

    @Test
    void resourceOwnershipRejectsMismatchedPathUserId() {
        String base = "http://localhost:" + port;
        UserEntity otherUser = new UserEntity();
        String nonce = UUID.randomUUID().toString().replace("-", "");
        otherUser.setCiHash(nonce + nonce);
        otherUser.setCiEncrypted("x".getBytes());
        otherUser.setPhoneHash("d" + nonce + "d".repeat(31));
        otherUser.setPhoneEncrypted("y".getBytes());
        otherUser.setStatus(UserEntity.UserStatus.ACTIVE);
        otherUser.setFailedLoginCount(0);
        otherUser.setCreatedAt(LocalDateTime.now());
        otherUser.setUpdatedAt(LocalDateTime.now());
        String other = userRepository.save(otherUser).getUserId().toString();

        ResponseEntity<String> tokenEntity = restTemplate.postForEntity(
                base + "/api/v1/auth/token",
                new HttpEntity<>("{\"userId\":\"" + userId + "\"}", jsonHeaders()),
                String.class
        );
        Assertions.assertTrue(tokenEntity.getStatusCode().is2xxSuccessful());
        String token = extractToken(tokenEntity.getBody());

        HttpHeaders authHeaders = jsonHeaders();
        authHeaders.setBearerAuth(token);
        HttpClientErrorException ex = Assertions.assertThrows(HttpClientErrorException.class, () ->
                restTemplate.exchange(
                        base + "/api/v1/persona/" + other,
                        HttpMethod.GET,
                        new HttpEntity<>(authHeaders),
                        String.class
                ));
        Assertions.assertEquals(403, ex.getStatusCode().value());
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

    private String extractUserId(String json) {
        if (json == null) return "";
        String key = "\"userId\":\"";
        int idx = json.indexOf(key);
        if (idx < 0) return "";
        int start = idx + key.length();
        int end = json.indexOf("\"", start);
        return end > start ? json.substring(start, end) : "";
    }
}
