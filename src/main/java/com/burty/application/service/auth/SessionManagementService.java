/**
 *
 *
 * <pre>
 * <b>Description  : 인증 애플리케이션 서비스 (SessionManagementService)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.application.service.auth
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
package com.burty.application.service.auth;

import com.burty.application.dto.auth.SessionResponse;
import com.burty.application.dto.auth.TokenPairMapper;
import com.burty.application.dto.auth.TokenPairResponse;
import com.burty.application.port.in.auth.SessionManagementUseCase;
import com.burty.core.error.enums.ErrorCode;
import com.burty.core.exception.BusinessException;
import com.burty.domain.auth.entity.UserSessionEntity;
import com.burty.domain.auth.repository.UserSessionRepository;
import com.burty.security.RefreshTokenService;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SessionManagementService implements SessionManagementUseCase {

  private final UserSessionRepository userSessionRepository;
  private final RefreshTokenService refreshTokenService;

  @Override
  @Transactional
  public TokenPairResponse createSession(String userId, String deviceId) {
    return TokenPairMapper.toResponse(refreshTokenService.issueNewSession(userId, deviceId));
  }

  @Override
  @Transactional
  public TokenPairResponse refreshSession(String refreshToken) {
    return TokenPairMapper.toResponse(refreshTokenService.rotate(refreshToken));
  }

  @Override
  @Transactional(readOnly = true)
  public List<SessionResponse> listActiveSessions(String userId) {
    return userSessionRepository.findByUserIdAndRevokedAtIsNull(Long.parseLong(userId)).stream()
        .map(this::toSessionResponse)
        .toList();
  }

  @Override
  @Transactional
  public void revokeSession(String sessionId) {
    UserSessionEntity session =
        userSessionRepository
            .findById(Long.parseLong(sessionId))
            .orElseThrow(
                () -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND, "해당 세션을 찾을 수 없습니다."));
    if (session.getRevokedAt() == null) {
      session.setRevokedAt(LocalDateTime.now());
      userSessionRepository.save(session);
    }
  }

  @Override
  @Transactional
  public void revokeAllSessions(String userId) {
    refreshTokenService.revokeAllForUser(userId);
  }

  private SessionResponse toSessionResponse(UserSessionEntity session) {
    return new SessionResponse(
        String.valueOf(session.getSessionId()),
        String.valueOf(session.getUserId()),
        session.getDeviceId(),
        session.getCreatedAt(),
        session.getExpiresAt());
  }
}
