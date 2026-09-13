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

import com.burty.core.dto.response.ApiResponse;
import com.burty.core.error.enums.ErrorCode;
import com.burty.core.error.enums.ErrorCodeHttpStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthLevelInterceptor implements HandlerInterceptor {
  private final RiskProofService riskProofService;
  private final ObjectMapper objectMapper;

  public AuthLevelInterceptor(RiskProofService riskProofService, ObjectMapper objectMapper) {
    this.riskProofService = riskProofService;
    this.objectMapper = objectMapper;
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
        writeStepUpRequired(response);
        return false;
      }
    }

    if (authLevel.value() == RiskLevel.LEVEL_3) {
      String riskProof = request.getHeader("X-Risk-Proof");
      if (!riskProofService.verify(riskProof, userId, RiskLevel.LEVEL_3)) {
        writeStepUpRequired(response);
        return false;
      }
    }

    return true;
  }

  /**
   * 단계 인증이 필요하다는 응답.
   *
   * <p>예전에는 {@code sendError} 로 끝내 Spring 기본 오류 본문이 나갔다. 봉투도 오류 코드도 없어서 FE 는 이 403 을 로그인 만료와 구분하지
   * 못했고, 화면에는 "다시 로그인해주세요" 가 떴다. 그 문구가 연동 해제(#147)와 네 기능(#155)의 원인을 오래 가렸다.
   *
   * <p>어느 등급이 모자란지는 싣지 않는다. 화면이 할 일은 같고(단계 인증을 다시 받는다), 등급을 알려주면 어떤 동작이 어떤 등급인지 밖으로 드러난다.
   */
  private void writeStepUpRequired(HttpServletResponse response) throws IOException {
    response.setStatus(ErrorCodeHttpStatus.resolve(ErrorCode.STEP_UP_REQUIRED).value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    objectMapper.writeValue(
        response.getWriter(),
        ApiResponse.error(
            ErrorCode.STEP_UP_REQUIRED.getMessage(),
            String.valueOf(ErrorCode.STEP_UP_REQUIRED.getCode())));
  }
}
