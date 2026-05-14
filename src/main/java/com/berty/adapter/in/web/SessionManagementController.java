package com.berty.adapter.in.web;

import com.berty.adapter.in.web.dto.*;
import com.berty.core.dto.response.ApiResponse;
import com.berty.domain.entity.UserSessionEntity;
import com.berty.domain.repository.UserSessionRepository;
import com.berty.security.AuthLevel;
import com.berty.security.JwtTokenProvider;
import com.berty.security.RiskLevel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/berty/sessions")
@Tag(name = "BERTY Session Management", description = "refresh token 및 기기별 세션 관리 API")
public class SessionManagementController {
    private final UserSessionRepository userSessionRepository;
    private final JwtTokenProvider jwtTokenProvider;

    public SessionManagementController(UserSessionRepository userSessionRepository, JwtTokenProvider jwtTokenProvider) {
        this.userSessionRepository = userSessionRepository;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping
    @AuthLevel(RiskLevel.LEVEL_2)
    @Operation(summary = "세션 생성", description = "기기별 refresh token을 발급합니다.")
    public ApiResponse<TokenPairResponse> create(@RequestParam String userId, @RequestParam(required = false) String deviceId) {
        String refreshToken = issueRefreshToken(userId, deviceId);
        UserSessionEntity session = new UserSessionEntity();
        session.setUserId(userId);
        session.setDeviceId(deviceId);
        session.setRefreshTokenHash(sha256(refreshToken));
        session.setExpiresAt(LocalDateTime.now().plusDays(30));
        userSessionRepository.save(session);
        return ApiResponse.ok(new TokenPairResponse(jwtTokenProvider.generateToken(userId), refreshToken));
    }

    @PostMapping("/refresh")
    @Operation(summary = "access token 재발급", description = "refresh token으로 access token을 재발급합니다.")
    public ApiResponse<TokenResponse> refresh(@RequestBody RefreshTokenRequest request) {
        UserSessionEntity session = userSessionRepository.findByRefreshTokenHashAndRevokedAtIsNull(sha256(request.getRefreshToken()))
                .filter(s -> s.getExpiresAt().isAfter(LocalDateTime.now()))
                .orElseThrow();
        return ApiResponse.ok(new TokenResponse(jwtTokenProvider.generateToken(session.getUserId())));
    }

    @GetMapping
    @AuthLevel(RiskLevel.LEVEL_2)
    public ApiResponse<List<SessionResponse>> sessions(@RequestParam String userId) {
        return ApiResponse.ok(userSessionRepository.findByUserIdAndRevokedAtIsNull(userId).stream()
                .map(s -> new SessionResponse(s.getSessionId().toString(), s.getUserId(), s.getDeviceId(), s.getCreatedAt(), s.getExpiresAt()))
                .toList());
    }

    @DeleteMapping("/{sessionId}")
    @AuthLevel(RiskLevel.LEVEL_2)
    public ApiResponse<SimpleResultResponse> revoke(@PathVariable String sessionId) {
        UserSessionEntity session = userSessionRepository.findById(UUID.fromString(sessionId)).orElseThrow();
        session.setRevokedAt(LocalDateTime.now());
        userSessionRepository.save(session);
        return ApiResponse.ok(new SimpleResultResponse(true, "세션이 만료되었습니다."));
    }

    @DeleteMapping
    @AuthLevel(RiskLevel.LEVEL_3)
    public ApiResponse<SimpleResultResponse> revokeAll(@RequestParam String userId) {
        LocalDateTime now = LocalDateTime.now();
        for (UserSessionEntity session : userSessionRepository.findByUserIdAndRevokedAtIsNull(userId)) {
            session.setRevokedAt(now);
            userSessionRepository.save(session);
        }
        return ApiResponse.ok(new SimpleResultResponse(true, "전체 세션이 만료되었습니다."));
    }

    private String issueRefreshToken(String userId, String deviceId) {
        return "brt_" + Base64.getUrlEncoder().withoutPadding()
                .encodeToString((userId + ":" + deviceId + ":" + UUID.randomUUID()).getBytes(StandardCharsets.UTF_8));
    }

    private String sha256(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest((raw == null ? "" : raw).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashed) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }
}
