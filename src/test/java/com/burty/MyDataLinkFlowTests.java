package com.burty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.burty.adapter.out.mydata.standard.StandardBankFixtures;
import com.burty.domain.user.entity.UserEntity;
import com.burty.domain.user.repository.UserRepository;
import com.burty.security.RiskLevel;
import com.burty.security.RiskProofService;
import com.burty.support.IntegrationTestBase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 기관 연결 흐름을 처음부터 끝까지 태운다: 인가 → 모의 동의(또는 거절) → 콜백 → FE 로 돌아감 → 목록 → 자산 요약 → 해제.
 *
 * <p>예전에는 이 흐름이 끊겨 있었다. 인가 URL 은 실재하지 않는 주소였고, 콜백은 JSON 을 돌려줘 브라우저가 거기 멈췄고, 자산 요약은 연결 여부와 무관하게
 * 고정값이었다.
 *
 * <p>브라우저가 모의 동의 화면에서 하는 일은 {@code redirect_uri?code&state} 로 이동하는 것뿐이라, 그 이동을 직접 흉내 낸다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MyDataLinkFlowTests extends IntegrationTestBase {

  private static final String FRONTEND = "http://localhost:3000";
  private static final String INSTITUTION = "KB";

  @LocalServerPort private int port;
  @Autowired private UserRepository userRepository;
  @Autowired private RiskProofService riskProofService;

  private final ObjectMapper json = new ObjectMapper();
  private final RestTemplate http = withoutRedirects();
  private String userId;
  private String token;

  @BeforeEach
  void setUp() throws IOException {
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

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    String body =
        http.postForEntity(
                base() + "/api/v1/auth/token",
                new HttpEntity<>("{\"userId\":\"" + userId + "\"}", headers),
                String.class)
            .getBody();
    token = json.readTree(body).path("data").path("accessToken").asText();
    assertFalse(token.isBlank(), "테스트 토큰을 받지 못했다");
  }

  @Test
  @DisplayName("인가 URL 은 모의 동의 화면을 가리키고 기관코드·state·redirect_uri 를 싣는다")
  void authorizeUrlPointsToMockConsent() throws IOException {
    String authorizeUrl = authorize(INSTITUTION);

    assertTrue(authorizeUrl.startsWith(FRONTEND + "/mydata/mock-consent?"), authorizeUrl);
    Map<String, String> query = query(authorizeUrl);
    assertEquals(INSTITUTION, query.get("org_code"));
    assertNotNull(query.get("state"));
    assertTrue(query.get("redirect_uri").endsWith("/api/v1/mydata/oauth/callback"));
  }

  @Test
  @DisplayName("동의하면 연동되고 FE 연동 관리 화면으로 돌아가며, 자산 요약에 그 기관이 합산된다")
  void consentLinksAndReturnsToFrontend() throws IOException {
    String state = query(authorize(INSTITUTION)).get("state");

    ResponseEntity<Void> back = callback("code=mock-code&state=" + enc(state));

    assertEquals(302, back.getStatusCode().value());
    assertEquals(FRONTEND + "/mypage/institutions?linked=KB", location(back));
    assertTrue(get("/api/v1/mydata/institutions").contains("\"KB\""));

    JsonNode summary = data(get("/api/v1/assets/summary"));
    long expected =
        StandardBankFixtures.balanceOf("KB", StandardBankFixtures.checkingAccount("KB"))
            + StandardBankFixtures.balanceOf("KB", StandardBankFixtures.savingsAccount("KB"));
    assertEquals(1, summary.path("linkedInstitutionCount").asInt());
    assertEquals(expected, summary.path("totalAsset").asLong());
  }

  @Test
  @DisplayName("거절하면 연동되지 않고 사유와 함께 돌아가며, 같은 state 는 다시 쓸 수 없다")
  void denyReturnsWithReasonAndBurnsState() throws IOException {
    String state = query(authorize(INSTITUTION)).get("state");

    ResponseEntity<Void> denied = callback("error=access_denied&state=" + enc(state));

    assertEquals(302, denied.getStatusCode().value());
    assertEquals(
        FRONTEND + "/mypage/institutions?link_error=denied&institution=KB", location(denied));
    assertEquals(0, data(get("/api/v1/assets/summary")).path("linkedInstitutionCount").asInt());

    // 거절에 쓴 state 로 동의를 보내도 연동되면 안 된다.
    ResponseEntity<Void> replay = callback("code=mock-code&state=" + enc(state));
    assertEquals(FRONTEND + "/mypage/institutions?link_error=state", location(replay));
  }

  @Test
  @DisplayName("연결하지 않은 사용자의 자산 요약은 고정값이 아니라 미연동이다")
  void unlinkedUserSeesNoFixedValue() throws IOException {
    JsonNode summary = data(get("/api/v1/assets/summary"));

    // 예전에는 여기서 총자산 3억 2천만 원이 나왔다.
    assertEquals(0, summary.path("linkedInstitutionCount").asInt());
    assertEquals(0, summary.path("totalAsset").asLong());
  }

  @Test
  @DisplayName("연동을 해제하면 자산 요약도 미연동으로 돌아간다")
  void unlinkRemovesFromSummary() throws IOException {
    String state = query(authorize(INSTITUTION)).get("state");
    callback("code=mock-code&state=" + enc(state));
    assertEquals(1, data(get("/api/v1/assets/summary")).path("linkedInstitutionCount").asInt());

    HttpHeaders headers = authHeaders();
    headers.set("X-Risk-Proof", riskProofService.issue(userId, RiskLevel.LEVEL_2));
    http.exchange(
        base() + "/api/v1/mydata/institutions/" + INSTITUTION,
        HttpMethod.DELETE,
        new HttpEntity<>(headers),
        String.class);

    assertEquals(0, data(get("/api/v1/assets/summary")).path("linkedInstitutionCount").asInt());
  }

  private String authorize(String institution) throws IOException {
    HttpHeaders headers = authHeaders();
    // 브라우저라면 Origin 이 붙지만 JDK HTTP 클라이언트는 그 헤더를 조용히 버린다. FE 가 실제로
    // 보내는 X-Frontend-Origin 을 쓴다.
    headers.set("X-Frontend-Origin", FRONTEND);
    String body =
        http.exchange(
                base() + "/api/v1/mydata/institutions/" + institution + "/authorize",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class)
            .getBody();
    return data(body).path("authorizeUrl").asText();
  }

  private ResponseEntity<Void> callback(String query) {
    // 브라우저는 모의 동의 화면(FE)에서 이 주소로 이동한다. 교차 출처 이동이라 Referer 에는 FE
    // 출처만 실린다.
    HttpHeaders headers = new HttpHeaders();
    headers.set(HttpHeaders.REFERER, FRONTEND + "/");
    return http.exchange(
        base() + "/api/v1/mydata/oauth/callback?" + query,
        HttpMethod.GET,
        new HttpEntity<>(headers),
        Void.class);
  }

  private String get(String path) {
    return http.exchange(
            base() + path, HttpMethod.GET, new HttpEntity<>(authHeaders()), String.class)
        .getBody();
  }

  private JsonNode data(String body) throws IOException {
    return json.readTree(body).path("data");
  }

  private static String location(ResponseEntity<?> response) {
    return String.valueOf(response.getHeaders().getLocation());
  }

  private static Map<String, String> query(String url) {
    Map<String, String> values = new HashMap<>();
    UriComponentsBuilder.fromUriString(url)
        .build()
        .getQueryParams()
        .forEach(
            (key, list) -> values.put(key, URLDecoder.decode(list.get(0), StandardCharsets.UTF_8)));
    return values;
  }

  private static String enc(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }

  private HttpHeaders authHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    return headers;
  }

  private String base() {
    return "http://localhost:" + port;
  }

  private static RestTemplate withoutRedirects() {
    // 기본 클라이언트는 302 를 따라가 버린다. 어디로 보내는지를 봐야 하므로 멈춘다.
    return new RestTemplate(
        new SimpleClientHttpRequestFactory() {
          @Override
          protected void prepareConnection(HttpURLConnection connection, String method)
              throws IOException {
            super.prepareConnection(connection, method);
            connection.setInstanceFollowRedirects(false);
          }
        });
  }
}
