/**
 *
 *
 * <pre>
 * <b>Description  : 설정 서블릿 필터 (AuthRateLimitFilter)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.config
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
package com.burty.config;

import com.burty.adapter.out.store.RateLimitStore;
import com.burty.core.dto.response.ApiResponse;
import com.burty.core.error.enums.ErrorCode;
import com.burty.security.JwtTokenProvider;
import com.burty.util.IpUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 요청 제한 필터.
 *
 * <p>예전에는 {@code /auth}, {@code /admin/auth}, {@code /sessions} 세 경로에만, 그것도 IP 당 분당 60회 고정으로 걸려
 * 있었다. 문제는 두 가지였다.
 *
 * <ul>
 *   <li>비용이 큰 엔드포인트(이체, 마이데이터 동기화, AI 상담)가 아예 무제한이었다. 인증만 통과하면 은행 API 를 무한정 호출할 수 있었다.
 *   <li>IP 단위라서 같은 회사·통신사 NAT 뒤의 정상 사용자들이 서로를 막았고, 반대로 IP 를 바꾸는 공격자는 그냥 통과했다.
 * </ul>
 *
 * <p>지금은 규칙 테이블로 경로별 한도를 정의하고, 인증된 요청은 <b>사용자 단위</b>로, 미인증 요청은 IP 단위로 계산한다.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class AuthRateLimitFilter extends OncePerRequestFilter {

  /** 경로 접두사별 한도. 첫 매칭 규칙이 적용되므로 더 구체적인 경로를 앞에 둔다. */
  private static final List<Rule> RULES =
      List.of(
          // 인증 — 크리덴셜 스터핑 대상. 미인증 트래픽이라 IP 단위.
          new Rule("/api/v1/auth", 60, 60_000L),
          new Rule("/api/v1/admin/auth", 20, 60_000L),
          new Rule("/api/v1/sessions", 60, 60_000L),
          // 돈이 움직이는 경로 — 가장 좁게.
          new Rule("/api/v1/finance/transfer", 10, 60_000L),
          new Rule("/api/v1/transactions/sync", 6, 60_000L),
          // 외부 API 비용이 큰 경로.
          new Rule("/api/v1/mydata", 30, 60_000L),
          new Rule("/api/v1/external", 30, 60_000L),
          new Rule("/api/v1/consult", 20, 60_000L),
          new Rule("/api/v1/voice", 20, 60_000L),
          // 그 외 API 전반의 안전망.
          new Rule("/api/v1", 300, 60_000L));

  private final ObjectMapper objectMapper;
  private final BurtyApiProperties apiProperties;
  private final RateLimitStore rateLimitStore;
  private final JwtTokenProvider jwtTokenProvider;

  public AuthRateLimitFilter(
      ObjectMapper objectMapper,
      BurtyApiProperties apiProperties,
      RateLimitStore rateLimitStore,
      JwtTokenProvider jwtTokenProvider) {
    this.objectMapper = objectMapper;
    this.apiProperties = apiProperties;
    this.rateLimitStore = rateLimitStore;
    this.jwtTokenProvider = jwtTokenProvider;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    if (!apiProperties.getRateLimit().isEnabled()) {
      return true;
    }
    String uri = request.getRequestURI();
    return uri == null || matchingRule(uri) == null;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    Rule rule = matchingRule(request.getRequestURI());
    if (rule == null) {
      filterChain.doFilter(request, response);
      return;
    }

    String key = rule.prefix() + "|" + request.getMethod() + "|" + subject(request);
    if (!rateLimitStore.tryConsume(key, rule.maxRequests(), rule.windowMillis())) {
      response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
      response.setContentType(MediaType.APPLICATION_JSON_VALUE);
      response.setHeader("Retry-After", String.valueOf(rule.windowMillis() / 1000));
      objectMapper.writeValue(
          response.getWriter(),
          ApiResponse.error(
              ErrorCode.TOO_MANY_REQUESTS.getMessage(),
              String.valueOf(ErrorCode.TOO_MANY_REQUESTS.getCode())));
      return;
    }
    filterChain.doFilter(request, response);
  }

  /**
   * 제한 주체.
   *
   * <p>인증된 요청은 사용자 단위여야 한다. IP 단위로만 세면 NAT 뒤의 정상 사용자끼리 서로를 막고, IP 를 바꾸는 쪽은 못 막는다.
   *
   * <p><b>토큰은 반드시 검증하고 쓴다.</b> 예전에는 이 필터가 JWT 필터보다 앞에서 돈다는 이유로 토큰 문자열을 그대로 주체로 삼았다. 그래서 아무 문자열이나
   * {@code Authorization: Bearer} 로 붙이면 매번 새 버킷이 생겼고, 로그인 엔드포인트는 토큰이 필요 없으니 크리덴셜 스터핑 방어(분당 60건)가
   * 사실상 없는 것과 같았다.
   *
   * <p>서명이 맞지 않으면 익명으로 보고 IP 로 센다. 서명 검증은 HMAC 한 번이라 앞단에서 돌려도 부담이 없고, 어차피 뒤의 JWT 필터가 같은 일을 한다.
   */
  private String subject(HttpServletRequest request) {
    String header = request.getHeader("Authorization");
    if (header != null && header.startsWith("Bearer ") && header.length() > 7) {
      String token = header.substring(7);
      if (jwtTokenProvider.validateToken(token)) {
        try {
          String userId = jwtTokenProvider.getUserId(token);
          if (userId != null && !userId.isBlank()) {
            return "u:" + userId;
          }
        } catch (RuntimeException ignored) {
          // 서명은 맞는데 클레임을 못 읽는 경우. 익명으로 떨어뜨린다.
        }
      }
    }
    return "ip:" + IpUtil.getClientIp(request);
  }

  private static Rule matchingRule(String uri) {
    for (Rule rule : RULES) {
      if (uri.startsWith(rule.prefix())) {
        return rule;
      }
    }
    return null;
  }

  private record Rule(String prefix, int maxRequests, long windowMillis) {}
}
