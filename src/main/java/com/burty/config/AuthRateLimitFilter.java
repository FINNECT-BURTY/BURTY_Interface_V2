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
import com.burty.util.IpUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** 인증 관련 경로에 IP 기반 rate limit 을 적용합니다. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class AuthRateLimitFilter extends OncePerRequestFilter {

  private static final int MAX_REQUESTS_PER_WINDOW = 60;
  private static final long WINDOW_MILLIS = 60_000L;

  private final ObjectMapper objectMapper;
  private final BurtyApiProperties apiProperties;
  private final RateLimitStore rateLimitStore;

  public AuthRateLimitFilter(
      ObjectMapper objectMapper, BurtyApiProperties apiProperties, RateLimitStore rateLimitStore) {
    this.objectMapper = objectMapper;
    this.apiProperties = apiProperties;
    this.rateLimitStore = rateLimitStore;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    if (!apiProperties.getRateLimit().isEnabled()) {
      return true;
    }
    String uri = request.getRequestURI();
    return uri == null
        || (!uri.startsWith("/api/v1/auth")
            && !uri.startsWith("/api/v1/admin/auth")
            && !uri.startsWith("/api/v1/sessions"));
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String key = IpUtil.getClientIp(request) + ":" + request.getMethod();
    if (!rateLimitStore.tryConsume(key, MAX_REQUESTS_PER_WINDOW, WINDOW_MILLIS)) {
      response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
      response.setContentType(MediaType.APPLICATION_JSON_VALUE);
      objectMapper.writeValue(
          response.getWriter(),
          ApiResponse.error(
              ErrorCode.TOO_MANY_REQUESTS.getMessage(),
              String.valueOf(ErrorCode.TOO_MANY_REQUESTS.getCode())));
      return;
    }
    filterChain.doFilter(request, response);
  }
}
