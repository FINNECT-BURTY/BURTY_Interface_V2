/**
 *
 *
 * <pre>
 * <b>Description  : 인증 API 컨트롤러 (SessionManagementController)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.adapter.in.web.auth
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
package com.burty.adapter.in.web.auth;

import com.burty.application.dto.auth.RefreshTokenRequest;
import com.burty.application.dto.auth.SessionResponse;
import com.burty.application.dto.auth.TokenPairResponse;
import com.burty.application.dto.shared.SimpleResultResponse;
import com.burty.application.port.in.auth.SessionManagementUseCase;
import com.burty.core.annotation.CurrentUserId;
import com.burty.core.controller.BaseController;
import com.burty.core.dto.response.ApiResponse;
import com.burty.security.AuthLevel;
import com.burty.security.RiskLevel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 사용자 세션(=refresh token row) 관리 — 토큰 발급/회전/폐기는 RefreshTokenService 가 단일 책임. 이 컨트롤러는 사용자가 "내 기기 보기 /
 * 특정 기기 로그아웃 / 전체 로그아웃" 같은 세션 관리 UI 를 지원하기 위해 존재다.
 *
 * <p>일반 인증 흐름은 refresh 는 POST /api/v1/auth/refresh 를 사용한다. 이 컨트롤러의 POST /sessions/refresh 는 동일 동작
 * 노출되는 호환용 경로.
 */
@RestController
@RequestMapping("/sessions")
@RequiredArgsConstructor
@Tag(name = "BURTY Session Management", description = "사용자 활성 세션(refresh token) 관리 API")
public class SessionManagementController extends BaseController {

  private final SessionManagementUseCase sessionManagementUseCase;

  @PostMapping
  @AuthLevel(RiskLevel.LEVEL_2)
  @Operation(summary = "세션 생성", description = "사용자+기기 refresh token 발급 (기기 등록·관리용).")
  public ApiResponse<TokenPairResponse> create(
      @CurrentUserId String userId, @RequestParam(required = false) String deviceId) {
    return ApiResponse.ok(sessionManagementUseCase.createSession(userId, deviceId));
  }

  @PostMapping("/refresh")
  @Operation(
      summary = "access + refresh token 재발급 (호환 경로)",
      description = "POST /api/v1/auth/refresh 와 동일. 새 코드에서는 /auth/refresh 사용 권장.")
  public ApiResponse<TokenPairResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
    return ApiResponse.ok(sessionManagementUseCase.refreshSession(request.refreshToken()));
  }

  @GetMapping
  @AuthLevel(RiskLevel.LEVEL_2)
  @Operation(summary = "활성 세션 목록", description = "사용자의 revoke 되지 않은 세션을 조회합니다.")
  public ApiResponse<List<SessionResponse>> sessions(@CurrentUserId String userId) {
    return ApiResponse.ok(sessionManagementUseCase.listActiveSessions(userId));
  }

  @DeleteMapping("/{sessionId}")
  @AuthLevel(RiskLevel.LEVEL_2)
  @Operation(summary = "특정 세션 종료", description = "sessionId 로 해당 기기의 세션 하나만 종료합니다.")
  public ApiResponse<SimpleResultResponse> revoke(@PathVariable String sessionId) {
    sessionManagementUseCase.revokeSession(sessionId);
    return ApiResponse.ok(new SimpleResultResponse(true, "세션이 만료되었습니다."));
  }

  @DeleteMapping
  @AuthLevel(RiskLevel.LEVEL_3)
  @Operation(summary = "전체 세션 종료", description = "사용자의 모든 활성 세션을 한 번에 종료합니다. (비밀번호 변경, 도난 의심 등)")
  public ApiResponse<SimpleResultResponse> revokeAll(@CurrentUserId String userId) {
    sessionManagementUseCase.revokeAllSessions(userId);
    return ApiResponse.ok(new SimpleResultResponse(true, "전체 세션이 만료되었습니다."));
  }
}
