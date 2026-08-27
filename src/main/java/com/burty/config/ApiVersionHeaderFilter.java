package com.burty.config;

import com.burty.core.constants.ApiVersions;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 응답에 처리한 API 버전을 표시한다.
 *
 * <p>클라이언트가 의도한 버전을 쓰고 있는지 확인할 수 있어야 한다. 버전을 올릴 때 구버전 응답에 {@code Deprecation}/{@code Sunset} 헤더를
 * 붙이면 클라이언트가 로그로 감지할 수 있다. 조용히 끊고 나서 통보하는 것보다 낫다.
 */
@Component
public class ApiVersionHeaderFilter extends OncePerRequestFilter {

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String uri = request.getRequestURI();
    return uri == null || !uri.startsWith("/api/");
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String uri = request.getRequestURI();
    if (uri.startsWith(ApiVersions.V2)) {
      response.setHeader(ApiVersions.VERSION_HEADER, "2");
    } else if (uri.startsWith(ApiVersions.V1)) {
      response.setHeader(ApiVersions.VERSION_HEADER, "1");
      // v2 도입 시 아래 두 줄의 주석을 풀고 폐기일을 지정한다.
      // response.setHeader(ApiVersions.DEPRECATION_HEADER, "true");
      // response.setHeader(ApiVersions.SUNSET_HEADER, "Wed, 31 Dec 2026 23:59:59 GMT");
    }
    filterChain.doFilter(request, response);
  }
}
