package com.burty;

import com.burty.domain.user.entity.UserEntity;
import com.burty.domain.user.repository.UserRepository;
import com.burty.security.RiskLevel;
import com.burty.security.RiskProofService;
import java.time.LocalDateTime;
import java.util.UUID;
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
import org.springframework.web.client.RestTemplate;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MyDataOpenBankingE2ETests {

  @LocalServerPort private int port;
  @Autowired private UserRepository userRepository;
  @Autowired private RiskProofService riskProofService;
  private final RestTemplate restTemplate = new RestTemplate();
  private String userId;
  private String token;

  @BeforeEach
  void setUp() {
    String nonce = UUID.randomUUID().toString().replace("-", "");
    UserEntity user = new UserEntity();
    user.setCiHash(nonce + nonce);
    user.setCi("ci-" + nonce);
    user.setPhoneHash(nonce + "0".repeat(32));
    user.setPhone("01012345678");
    user.setStatus(UserEntity.UserStatus.ACTIVE);
    user.setFailedLoginCount(0);
    user.setCreatedAt(LocalDateTime.now());
    user.setUpdatedAt(LocalDateTime.now());
    userId = userRepository.save(user).getUserId().toString();

    ResponseEntity<String> tokenEntity =
        restTemplate.postForEntity(
            base() + "/api/v1/auth/token",
            new HttpEntity<>("{\"userId\":\"" + userId + "\"}", jsonHeaders()),
            String.class);
    token = extractToken(tokenEntity.getBody());
  }

  @Test
  void myDataOAuthCallbackLinksInstitution() {
    ResponseEntity<String> callback =
        restTemplate.postForEntity(
            base() + "/api/v1/mydata/oauth/callback",
            new HttpEntity<>(
                "{\"userId\":\"" + userId + "\",\"code\":\"stub-code\"}", authHeaders()),
            String.class);
    Assertions.assertTrue(callback.getStatusCode().is2xxSuccessful());
    Assertions.assertNotNull(callback.getBody());
    Assertions.assertTrue(callback.getBody().contains("\"flag\":\"linked\""));
    Assertions.assertTrue(callback.getBody().contains("\"value\":true"));
  }

  @Test
  void myDataTransmissionCreateAndList() {
    ResponseEntity<String> create =
        restTemplate.postForEntity(
            base() + "/api/v1/mydata/transmission/requests",
            new HttpEntity<>(
                "{\"userId\":\""
                    + userId
                    + "\",\"institutionCode\":\"MYDATA\",\"scope\":\"asset.read\"}",
                level2Headers()),
            String.class);
    Assertions.assertTrue(create.getStatusCode().is2xxSuccessful());

    ResponseEntity<String> list =
        restTemplate.exchange(
            base() + "/api/v1/mydata/transmission/requests?userId=" + userId,
            HttpMethod.GET,
            new HttpEntity<>(authHeaders()),
            String.class);
    Assertions.assertTrue(list.getStatusCode().is2xxSuccessful());
    Assertions.assertNotNull(list.getBody());
    Assertions.assertFalse(list.getBody().contains("\"data\":[]"));
  }

  @Test
  void openBankingOAuthCallbackAndAccounts() {
    ResponseEntity<String> callback =
        restTemplate.postForEntity(
            base()
                + "/api/v1/external/openbanking/oauth/callback?userId="
                + userId
                + "&code=stub-code",
            new HttpEntity<>(authHeaders()),
            String.class);
    Assertions.assertTrue(callback.getStatusCode().is2xxSuccessful());
    Assertions.assertTrue(callback.getBody().contains("\"flag\":\"linked\""));
    Assertions.assertTrue(callback.getBody().contains("\"value\":true"));

    ResponseEntity<String> accounts =
        restTemplate.exchange(
            base() + "/api/v1/external/openbanking/accounts?userId=" + userId,
            HttpMethod.GET,
            new HttpEntity<>(authHeaders()),
            String.class);
    Assertions.assertTrue(accounts.getStatusCode().is2xxSuccessful());
    Assertions.assertTrue(accounts.getBody().contains("DemoBank"));
  }

  @Test
  void myDataAuthorizeUrlContainsClientId() {
    ResponseEntity<String> authorize =
        restTemplate.exchange(
            base() + "/api/v1/mydata/oauth/authorize?userId=" + userId,
            HttpMethod.GET,
            new HttpEntity<>(authHeaders()),
            String.class);
    Assertions.assertTrue(authorize.getStatusCode().is2xxSuccessful());
    Assertions.assertTrue(
        authorize.getBody().contains("client_id") || authorize.getBody().contains("client-id"));
  }

  private String base() {
    return "http://localhost:" + port;
  }

  private HttpHeaders jsonHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    return headers;
  }

  private HttpHeaders authHeaders() {
    HttpHeaders headers = jsonHeaders();
    headers.setBearerAuth(token);
    return headers;
  }

  private HttpHeaders level2Headers() {
    HttpHeaders headers = authHeaders();
    headers.set("X-Risk-Proof", riskProofService.issue(userId, RiskLevel.LEVEL_2));
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
