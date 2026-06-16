/**
 *
 *
 * <pre>
 * <b>Description  : 보안 인터셉터 (AuthLevelInterceptor)</b>
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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthLevelInterceptor implements HandlerInterceptor {
  private final RiskProofService riskProofService;

  public AuthLevelInterceptor(RiskProofService riskProofService) {
    this.riskProofService = riskProofService;
  }

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws Exception {
    if (!(handler instanceof HandlerMethod handlerMethod)) {
      return true;
    }

    AuthLevel authLevel = handlerMethod.getMethodAnnotation(AuthLevel.class);
    if (authLevel == null) {
      authLevel = handlerMethod.getBeanType().getAnnotation(AuthLevel.class);
    }
    if (authLevel == null) {
      return true;
    }

    var authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
      response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication is required");
      return false;
    }
    String userId = String.valueOf(authentication.getPrincipal());

    if (authLevel.value() == RiskLevel.LEVEL_2) {
      String riskProof = request.getHeader("X-Risk-Proof");
      if (!riskProofService.verify(riskProof, userId, RiskLevel.LEVEL_2)) {
        response.sendError(
            HttpServletResponse.SC_FORBIDDEN, "Verified level-2 risk proof is required");
        return false;
      }
    }

    if (authLevel.value() == RiskLevel.LEVEL_3) {
      String riskProof = request.getHeader("X-Risk-Proof");
      if (!riskProofService.verify(riskProof, userId, RiskLevel.LEVEL_3)) {
        response.sendError(
            HttpServletResponse.SC_FORBIDDEN, "Verified level-3 risk proof is required");
        return false;
      }
    }

    return true;
  }
}
