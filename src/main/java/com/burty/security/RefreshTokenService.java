/**
 *
 *
 * <pre>
 * <b>Description  : 보안 애플리케이션 서비스 (RefreshTokenService)</b>
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

import com.burty.config.JwtProperties;
import com.burty.core.error.enums.ErrorCode;
import com.burty.core.exception.BusinessException;
import com.burty.domain.auth.entity.UserSessionEntity;
import com.burty.domain.auth.repository.UserSessionRepository;
import com.burty.util.AccountNumberHasher;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Refresh Token 발급 / 회전 / 폐기를 담당.
 *
 * <p>토큰 자체는 opaque random (32 bytes → base64url), DB 에는 SHA-256 hash 만 저장. - 회전(rotate): 매 refresh
 * 호출 시 새 refresh token 발급 + 기존 token revoke. - 재사용 탐지: 이미 revoke 된 token 으로 refresh 시 → 도난 의심 → 해당
 * user 전체 세션 강제 종료.
 */
@Service
public class RefreshTokenService {

  private static final String TOKEN_PREFIX = "brt_";
  private static final int RAW_BYTES = 32;

  private final UserSessionRepository sessionRepository;
  private final JwtTokenProvider jwtTokenProvider;
  private final JwtProperties jwtProperties;
  private final AccountNumberHasher accountNumberHasher;

  /**
   * 세션 강제 종료. 별도 빈이라야 {@code REQUIRES_NEW} 가 적용된다.
   *
   * <p>재사용을 감지하면 세션을 끊고 예외를 던져 요청을 거절한다. 같은 트랜잭션에서 끊으면 그 예외가 끊은 것까지 되돌려, 응답만 "모든 세션을 종료했습니다" 이고
   * 실제로는 아무 일도 일어나지 않는다.
   */
  private final SessionRevoker sessionRevoker;

  private final SecureRandom secureRandom = new SecureRandom();

  public RefreshTokenService(
      UserSessionRepository sessionRepository,
      JwtTokenProvider jwtTokenProvider,
      JwtProperties jwtProperties,
      AccountNumberHasher accountNumberHasher,
      SessionRevoker sessionRevoker) {
    this.sessionRepository = sessionRepository;
    this.jwtTokenProvider = jwtTokenProvider;
    this.jwtProperties = jwtProperties;
    this.accountNumberHasher = accountNumberHasher;
    this.sessionRevoker = sessionRevoker;
  }

  /** 로그인 직후 호출. access + refresh 쌍 발급, 새 session row 생성. */
  @Transactional
  public TokenPair issueNewSession(String userId, String deviceId) {
    Long numericUserId = parseUserId(userId);
    String rawRefresh = generateToken();
    UserSessionEntity session = new UserSessionEntity();
    session.setUserId(numericUserId);
    session.setDeviceId(deviceId);
    session.setRefreshTokenHash(accountNumberHasher.hash(rawRefresh));
    session.setExpiresAt(
        LocalDateTime.now().plusSeconds(jwtProperties.getRefreshExpirationSeconds()));
    sessionRepository.save(session);

    return new TokenPair(
        jwtTokenProvider.generateToken(userId),
        rawRefresh,
        jwtProperties.getExpirationSeconds(),
        jwtProperties.getRefreshExpirationSeconds());
  }

  private Long parseUserId(String userId) {
    try {
      return Long.parseLong(userId);
    } catch (NumberFormatException e) {
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "userId 형식이 올바르지 않습니다.");
    }
  }

  /** Refresh token 으로 새 쌍 발급 (rotation). 재사용 시 모든 세션 강제 종료. */
  @Transactional
  public TokenPair rotate(String refreshToken) {
    if (refreshToken == null || refreshToken.isBlank()) {
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "refresh token 이 필요합니다.");
    }
    String hash = accountNumberHasher.hash(refreshToken);
    Optional<UserSessionEntity> maybe = sessionRepository.findByRefreshTokenHash(hash);
    if (maybe.isEmpty()) {
      throw new BusinessException(ErrorCode.INVALID_TOKEN, "유효하지 않은 refresh token 입니다.");
    }
    UserSessionEntity session = maybe.get();

    // 도난 의심: 이미 revoke 된 token 이 다시 들어옴 → 모든 세션 강제 종료
    if (session.getRevokedAt() != null) {
      sessionRevoker.revokeAllForUser(session.getUserId());
      throw new BusinessException(
          ErrorCode.FORBIDDEN, "refresh token 재사용이 감지되었습니다. 보안을 위해 모든 세션을 종료했습니다.");
    }
    if (session.getExpiresAt().isBefore(LocalDateTime.now())) {
      throw new BusinessException(ErrorCode.EXPIRED_TOKEN, "refresh token 이 만료되었습니다.");
    }

    String userId = String.valueOf(session.getUserId());
    String deviceId = session.getDeviceId();

    // 조건부 UPDATE 로 한쪽만 회전에 성공하게 한다. 읽어서 확인하고 쓰면, 같은 token 으로
    // 두 요청이 동시에 들어왔을 때 둘 다 통과해 각자 새 세션을 받는다 — 재사용 탐지가
    // 정확히 그 상황을 잡으라고 있는 것인데 그때 뚫린다.
    if (sessionRepository.revokeIfActive(session.getSessionId(), LocalDateTime.now()) == 0) {
      sessionRevoker.revokeAllForUser(session.getUserId());
      throw new BusinessException(
          ErrorCode.FORBIDDEN, "refresh token 재사용이 감지되었습니다. 보안을 위해 모든 세션을 종료했습니다.");
    }

    return issueNewSession(userId, deviceId);
  }

  /** 로그아웃 시 호출. 해당 refresh token 하나만 revoke (없으면 조용히 무시). */
  @Transactional
  public void revoke(String refreshToken) {
    if (refreshToken == null || refreshToken.isBlank()) return;
    sessionRepository
        .findByRefreshTokenHashAndRevokedAtIsNull(accountNumberHasher.hash(refreshToken))
        .ifPresent(
            s -> {
              s.setRevokedAt(LocalDateTime.now());
              sessionRepository.save(s);
            });
  }

  /** 특정 사용자의 모든 활성 세션 종료 (비밀번호 변경 / 도난 의심 등). */
  @Transactional
  public void revokeAllForUser(String userId) {
    sessionRepository.revokeAllActive(parseUserId(userId), LocalDateTime.now());
  }

  private String generateToken() {
    byte[] bytes = new byte[RAW_BYTES];
    secureRandom.nextBytes(bytes);
    return TOKEN_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  public record TokenPair(
      String accessToken,
      String refreshToken,
      long accessExpiresInSeconds,
      long refreshExpiresInSeconds) {}
}
