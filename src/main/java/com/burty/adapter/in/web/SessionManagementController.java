package com.burty.adapter.in.web;

import com.burty.core.controller.BaseController;

import com.burty.adapter.in.web.dto.*;
import com.burty.core.dto.response.ApiResponse;
import com.burty.core.error.enums.ErrorCode;
import com.burty.core.exception.BusinessException;
import com.burty.domain.entity.UserSessionEntity;
import com.burty.domain.repository.UserSessionRepository;
import com.burty.security.AuthLevel;
import com.burty.security.RefreshTokenService;
import com.burty.security.RiskLevel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 사용자 세션(=refresh token row) 관리. 토큰 발급/회전/폐기는 RefreshTokenService 가 단일 책임.
 * 이 컨트롤러는 사용자가 "내 기기 보기 / 특정 기기 로그아웃 / 전체 로그아웃" 같은 세션 관리 UI 를
 * 지원하기 위해 존재한다.
 *
 * 표준 인증 흐름의 refresh 는 POST /api/v1/auth/refresh 를 사용한다. 이 컨트롤러의
 * POST /sessions/refresh 는 동일 동작을 호출하는 호환용 경로다.
 */
@RestController
@RequestMapping("/sessions")
@Tag(name = "BURTY Session Management", description = "사용자 활성 세션(refresh token) 관리 API")
public class SessionManagementController extends BaseController {
    private final UserSessionRepository userSessionRepository;
    private final RefreshTokenService refreshTokenService;

    public SessionManagementController(UserSessionRepository userSessionRepository,
                                       RefreshTokenService refreshTokenService) {
        this.userSessionRepository = userSessionRepository;
        this.refreshTokenService = refreshTokenService;
    }

    @PostMapping
    @AuthLevel(RiskLevel.LEVEL_2)
    @Operation(summary = "세션 생성", description = "지정한 사용자 + 기기 조합으로 새 refresh token 을 발급합니다. 일반 로그인 흐름에서는 사용하지 않고, 기기 추가 등록 / 관리 목적에서만 사용합니다.")
    public ApiResponse<TokenPairResponse> create(@RequestParam String userId,
                                                 @RequestParam(required = false) String deviceId) {
        RefreshTokenService.TokenPair pair = refreshTokenService.issueNewSession(userId, deviceId);
        return ApiResponse.ok(new TokenPairResponse(
                pair.accessToken(),
                pair.refreshToken(),
                pair.accessExpiresInSeconds(),
                pair.refreshExpiresInSeconds()
        ));
    }

    @PostMapping("/refresh")
    @Operation(summary = "access + refresh token 재발급 (호환 경로)", description = "POST /api/v1/auth/refresh 와 동일. 새 코드에서는 /auth/refresh 사용 권장.")
    public ApiResponse<TokenPairResponse> refresh(@RequestBody RefreshTokenRequest request) {
        RefreshTokenService.TokenPair pair = refreshTokenService.rotate(request.getRefreshToken());
        return ApiResponse.ok(new TokenPairResponse(
                pair.accessToken(),
                pair.refreshToken(),
                pair.accessExpiresInSeconds(),
                pair.refreshExpiresInSeconds()
        ));
    }

    @GetMapping
    @AuthLevel(RiskLevel.LEVEL_2)
    @Operation(summary = "활성 세션 목록", description = "사용자의 revoke 되지 않은 세션을 조회합니다.")
    public ApiResponse<List<SessionResponse>> sessions(@RequestParam String userId) {
        return ApiResponse.ok(userSessionRepository.findByUserIdAndRevokedAtIsNull(Long.parseLong(userId)).stream()
                .map(s -> new SessionResponse(String.valueOf(s.getSessionId()), String.valueOf(s.getUserId()), s.getDeviceId(), s.getCreatedAt(), s.getExpiresAt()))
                .toList());
    }

    @DeleteMapping("/{sessionId}")
    @AuthLevel(RiskLevel.LEVEL_2)
    @Operation(summary = "특정 세션 종료", description = "sessionId 로 해당 기기의 세션 하나만 종료합니다.")
    public ApiResponse<SimpleResultResponse> revoke(@PathVariable String sessionId) {
        UserSessionEntity session = userSessionRepository.findById(Long.parseLong(sessionId))
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND, "해당 세션을 찾을 수 없습니다."));
        if (session.getRevokedAt() == null) {
            session.setRevokedAt(LocalDateTime.now());
            userSessionRepository.save(session);
        }
        return ApiResponse.ok(new SimpleResultResponse(true, "세션이 만료되었습니다."));
    }

    @DeleteMapping
    @AuthLevel(RiskLevel.LEVEL_3)
    @Operation(summary = "전체 세션 종료", description = "사용자의 모든 활성 세션을 한 번에 종료합니다. (비밀번호 변경, 도난 의심 등)")
    public ApiResponse<SimpleResultResponse> revokeAll(@RequestParam String userId) {
        refreshTokenService.revokeAllForUser(userId);
        return ApiResponse.ok(new SimpleResultResponse(true, "전체 세션이 만료되었습니다."));
    }
}
