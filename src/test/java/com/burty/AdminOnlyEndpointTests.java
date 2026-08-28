package com.burty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.burty.security.RiskLevel;
import com.burty.security.RiskProofService;
import com.burty.support.IntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

/**
 * 관리자 전용 엔드포인트의 권한 경계.
 *
 * <p>{@code GET /api/v1/kpi/global} 은 컨트롤러 주석에 "관리자용" 이라 적혀 있었지만 경로가 {@code /api/v1/kpi} 라 {@code
 * /api/v1/** → authenticated()} 만 걸렸다. 붙어 있던 {@code @AuthLevel(LEVEL_3)} 은 단계 인증이지 권한이 아니라서, 인증된
 * 사용자면 누구나 전사 집계 지표를 읽을 수 있었다.
 *
 * <p>이 프로젝트는 메서드 보안({@code @EnableMethodSecurity})을 켜지 않았다. 즉 {@code @PreAuthorize} 를 붙여도 조용히 무시된다.
 * 권한은 {@code SecurityConfig} 의 매처로만 걸리므로, 매처가 실제로 동작하는지 여기서 확인한다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AdminOnlyEndpointTests extends IntegrationTestBase {

  @LocalServerPort private int port;

  /**
   * 단계 인증(LEVEL_3) 증명을 직접 발급한다.
   *
   * <p>이것 없이 요청하면 {@code @AuthLevel(LEVEL_3)} 인터셉터가 먼저 403 을 내서, 권한 매처가 있든 없든 결과가 같다. 그러면 테스트가 아무것도
   * 검증하지 않는다. 일반 사용자도 자기 패스키로 이 증명을 받을 수 있으므로, 여기까지 통과시킨 뒤에야 "관리자만" 이 지켜지는지 물을 수 있다.
   */
  @Autowired private RiskProofService riskProofService;

  /** 4xx 를 예외로 바꾸지 않는다. 여기서는 상태코드 자체가 검증 대상이다. */
  private final RestTemplate restTemplate = permissiveRestTemplate();

  private static RestTemplate permissiveRestTemplate() {
    RestTemplate template = new RestTemplate();
    template.setErrorHandler(
        new DefaultResponseErrorHandler() {
          @Override
          public boolean hasError(ClientHttpResponse response) {
            return false;
          }
        });
    return template;
  }

  @Test
  @DisplayName("단계 인증을 통과한 일반 사용자도 전사 KPI 를 읽을 수 없다")
  void globalKpiIsForbiddenForRegularUserWithRiskProof() {
    ResponseEntity<String> response =
        get("/api/v1/kpi/global", userToken(), riskProofService.issue("1", RiskLevel.LEVEL_3));

    assertEquals(
        HttpStatus.FORBIDDEN,
        response.getStatusCode(),
        "관리자용 지표가 일반 사용자에게 열려 있다: " + response.getBody());
  }

  @Test
  @DisplayName("토큰 없이도 전사 KPI 를 읽을 수 없다")
  void globalKpiIsRejectedWithoutToken() {
    ResponseEntity<String> response = get("/api/v1/kpi/global", null, null);

    // 익명 요청에는 403 이 나간다(401 이 아니다). 이 프로젝트의 기존 동작이라 그대로 둔다 —
    // 여기서 확인할 것은 "읽히지 않는다" 이지 상태코드의 종류가 아니다.
    assertTrue(
        response.getStatusCode().is4xxClientError(),
        "인증 없이 관리자용 지표를 읽었다: " + response.getStatusCode());
  }

  @Test
  @DisplayName("사용자 KPI 는 일반 사용자도 읽을 수 있다 (권한 매처가 과하게 걸리지 않았다)")
  void userKpiRemainsAccessible() {
    ResponseEntity<String> response = get("/api/v1/kpi/user/1", userToken(), null);

    assertEquals(HttpStatus.OK, response.getStatusCode(), "사용자 KPI 까지 막혔다: " + response.getBody());
  }

  private String userToken() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    ResponseEntity<String> issued =
        restTemplate.postForEntity(
            base() + "/api/v1/auth/token",
            new HttpEntity<>("{\"userId\":\"1\"}", headers),
            String.class);
    String body = issued.getBody();
    int start = body.indexOf("\"accessToken\":\"") + "\"accessToken\":\"".length();
    return body.substring(start, body.indexOf('"', start));
  }

  private ResponseEntity<String> get(String path, String token, String riskProof) {
    HttpHeaders headers = new HttpHeaders();
    if (token != null) {
      headers.set("Authorization", "Bearer " + token);
    }
    if (riskProof != null) {
      headers.set("X-Risk-Proof", riskProof);
    }
    return restTemplate.exchange(
        base() + path, HttpMethod.GET, new HttpEntity<>(headers), String.class);
  }

  private String base() {
    return "http://localhost:" + port;
  }
}
