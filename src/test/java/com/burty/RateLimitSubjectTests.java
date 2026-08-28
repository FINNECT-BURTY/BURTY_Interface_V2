package com.burty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.burty.adapter.out.store.RateLimitStore;
import com.burty.config.AuthRateLimitFilter;
import com.burty.config.BurtyApiProperties;
import com.burty.config.JwtProperties;
import com.burty.security.JwtTokenProvider;
import com.burty.util.IpUtil;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * 레이트리밋 주체 산정.
 *
 * <p>버킷 키가 요청자 마음대로 갈라지면 제한은 없는 것과 같다. 예전에는 두 가지로 갈랐다.
 *
 * <ul>
 *   <li>아무 문자열이나 {@code Authorization: Bearer} 로 붙이면 매번 새 버킷. 로그인 엔드포인트는 토큰이 필요 없으므로 크리덴셜 스터핑 방어가
 *       사실상 없었다.
 *   <li>{@code X-Forwarded-For} 를 직접 넣으면 원하는 IP 버킷. 전달 헤더를 검증 없이 믿었다.
 * </ul>
 */
class RateLimitSubjectTests {

  private static final String LOGIN = "/api/v1/auth/login";
  private static final String REAL_CLIENT = "203.0.113.9";

  private final List<String> keys = new ArrayList<>();
  private AuthRateLimitFilter filter;
  private JwtTokenProvider jwtTokenProvider;

  @BeforeEach
  void setUp() {
    keys.clear();
    // 신뢰 프록시 없음 = 전달 헤더를 보지 않는 기본 설정.
    IpUtil.configure(List.of());

    JwtProperties jwtProperties = new JwtProperties();
    jwtProperties.setSecret("burty-rate-limit-test-secret-key-long-enough-for-hmac-sha256");
    jwtTokenProvider = new JwtTokenProvider(jwtProperties);

    BurtyApiProperties apiProperties = new BurtyApiProperties();
    apiProperties.getRateLimit().setEnabled(true);

    RateLimitStore recorder =
        (key, maxRequests, windowMillis) -> {
          keys.add(key);
          return true;
        };
    filter =
        new AuthRateLimitFilter(
            JsonMapper.builder().build(), apiProperties, recorder, jwtTokenProvider);
  }

  @Test
  @DisplayName("위조 토큰을 아무리 바꿔도 버킷은 갈라지지 않는다")
  void forgedTokensShareTheAnonymousBucket() throws Exception {
    for (String token : List.of("random-aaa", "random-bbb", "random-ccc")) {
      send(request -> request.addHeader("Authorization", "Bearer " + token));
    }

    assertEquals(
        Set.of("/api/v1/auth|POST|ip:" + REAL_CLIENT),
        distinctSubjects(),
        "서명되지 않은 토큰으로 버킷이 갈라졌다 — 레이트리밋이 우회된다");
  }

  @Test
  @DisplayName("전달 헤더를 넣어도 신뢰 프록시가 아니면 무시한다")
  void forwardedHeadersAreIgnoredFromUntrustedSource() throws Exception {
    send(request -> request.addHeader("X-Forwarded-For", "1.2.3.4"));
    send(request -> request.addHeader("X-Real-IP", "5.6.7.8"));
    send(request -> request.addHeader("Proxy-Client-IP", "9.9.9.9"));

    assertEquals(
        Set.of("/api/v1/auth|POST|ip:" + REAL_CLIENT),
        distinctSubjects(),
        "클라이언트가 넣은 헤더로 IP 버킷이 갈라졌다");
  }

  @Test
  @DisplayName("신뢰 프록시를 지정하면 전달 헤더에서 클라이언트를 찾는다")
  void trustedProxyForwardedForIsHonoured() throws Exception {
    IpUtil.configure(List.of(REAL_CLIENT + "/32"));

    // 앞쪽 항목은 공격자가 얼마든지 채워 넣을 수 있다. 신뢰 프록시가 아닌 가장 오른쪽이 클라이언트다.
    send(request -> request.addHeader("X-Forwarded-For", "9.9.9.9, 198.51.100.7"));

    assertEquals(
        Set.of("/api/v1/auth|POST|ip:198.51.100.7"),
        distinctSubjects(),
        "신뢰 프록시 뒤에서 클라이언트 IP 를 잘못 골랐다");
  }

  @Test
  @DisplayName("서명이 유효한 토큰은 사용자 단위로 센다")
  void validTokenIsCountedPerUser() throws Exception {
    String tokenA = jwtTokenProvider.generateToken("1001");
    String tokenB = jwtTokenProvider.generateToken("1002");

    send(request -> request.addHeader("Authorization", "Bearer " + tokenA));
    send(request -> request.addHeader("Authorization", "Bearer " + tokenA));
    send(request -> request.addHeader("Authorization", "Bearer " + tokenB));

    assertEquals(
        Set.of("/api/v1/auth|POST|u:1001", "/api/v1/auth|POST|u:1002"), distinctSubjects());
    // 같은 사용자가 토큰을 재발급받아도 같은 버킷이어야 한다.
    String rotated = jwtTokenProvider.generateToken("1001");
    keys.clear();
    send(request -> request.addHeader("Authorization", "Bearer " + rotated));
    assertTrue(keys.get(0).endsWith("u:1001"), "토큰 재발급으로 버킷이 갈라졌다: " + keys.get(0));
  }

  private Set<String> distinctSubjects() {
    return new LinkedHashSet<>(keys);
  }

  private void send(java.util.function.Consumer<MockHttpServletRequest> customizer)
      throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", LOGIN);
    request.setRemoteAddr(REAL_CLIENT);
    customizer.accept(request);
    filter.doFilter(request, new MockHttpServletResponse(), (rq, rs) -> {});
  }
}
