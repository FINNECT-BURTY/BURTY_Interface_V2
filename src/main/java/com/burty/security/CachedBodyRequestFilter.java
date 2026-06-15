/**
 *
 *
 * <pre>
 * <b>Description  : 보안 서블릿 필터 (CachedBodyRequestFilter)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.security
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
package com.burty.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

/** JSON body 에 포함된 userId 등을 인터셉터에서 재읽을 수 있도록 요청 본문을 캐시합니다. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class CachedBodyRequestFilter extends OncePerRequestFilter {

  private static final int BODY_CACHE_LIMIT = 1024 * 1024;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    if (shouldWrap(request)) {
      filterChain.doFilter(new ContentCachingRequestWrapper(request, BODY_CACHE_LIMIT), response);
    } else {
      filterChain.doFilter(request, response);
    }
  }

  private static boolean shouldWrap(HttpServletRequest request) {
    String uri = request.getRequestURI();
    if (uri == null || !uri.startsWith("/api/v1/")) {
      return false;
    }
    String method = request.getMethod();
    if (!"POST".equals(method) && !"PUT".equals(method) && !"PATCH".equals(method)) {
      return false;
    }
    String contentType = request.getContentType();
    return contentType != null && contentType.toLowerCase().contains("application/json");
  }
}
