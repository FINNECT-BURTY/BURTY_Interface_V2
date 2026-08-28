/**
 *
 *
 * <pre>
 * <b>Description  : 보안 서블릿 필터 (JwtAuthenticationFilter)</b>
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

import com.burty.core.dto.response.ApiResponse;
import com.burty.core.error.enums.ErrorCode;
import com.burty.util.LoginFailLogUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.jspecify.annotations.NonNull;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
  private final JwtTokenProvider jwtTokenProvider;
  private final JwtBlacklistService jwtBlacklistService;
  private final LoginFailLogUtil loginFailLogUtil;
  private final ObjectMapper objectMapper;

  public JwtAuthenticationFilter(
      JwtTokenProvider jwtTokenProvider,
      JwtBlacklistService jwtBlacklistService,
      LoginFailLogUtil loginFailLogUtil,
      ObjectMapper objectMapper) {
    this.jwtTokenProvider = jwtTokenProvider;
    this.jwtBlacklistService = jwtBlacklistService;
    this.loginFailLogUtil = loginFailLogUtil;
    this.objectMapper = objectMapper;
  }

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {
    String token = extractToken(request);
    if (token != null) {
      if (jwtBlacklistService.isBlacklisted(token)) {
        loginFailLogUtil.logTokenFailure(request, "TOKEN_BLACKLISTED");
        writeUnauthorized(response, ErrorCode.INVALID_TOKEN);
        return;
      }
      if (!jwtTokenProvider.validateToken(token)) {
        loginFailLogUtil.logTokenFailure(request, "INVALID_TOKEN");
        writeUnauthorized(response, ErrorCode.EXPIRED_TOKEN);
        return;
      }
      String userId = jwtTokenProvider.getUserId(token);
      String role = jwtTokenProvider.getRole(token);
      UsernamePasswordAuthenticationToken authentication =
          new UsernamePasswordAuthenticationToken(
              userId, null, List.of(new SimpleGrantedAuthority(role)));
      SecurityContextHolder.getContext().setAuthentication(authentication);
    }
    filterChain.doFilter(request, response);
  }

  private void writeUnauthorized(HttpServletResponse response, ErrorCode errorCode)
      throws IOException {
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    objectMapper.writeValue(
        response.getWriter(),
        ApiResponse.error(errorCode.getMessage(), String.valueOf(errorCode.getCode())));
  }

  private String extractToken(HttpServletRequest request) {
    String header = request.getHeader("Authorization");
    if (header != null && header.startsWith("Bearer ")) {
      return header.substring(7);
    }
    Cookie[] cookies = request.getCookies();
    if (cookies != null) {
      for (Cookie c : cookies) {
        if (AuthCookies.ACCESS.equals(c.getName())
            && c.getValue() != null
            && !c.getValue().isBlank()) {
          return c.getValue();
        }
      }
    }
    return null;
  }
}
