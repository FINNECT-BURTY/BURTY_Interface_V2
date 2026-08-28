package com.burty.security;

import com.burty.domain.auth.repository.UserSessionRepository;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 세션 강제 종료.
 *
 * <p>별도 빈으로 둔 이유가 있다. 토큰 재사용을 감지하면 모든 세션을 끊고 <b>예외를 던져</b> 요청을 거절하는데, 같은 트랜잭션 안에서 끊으면 그 예외가 끊은 것까지
 * 되돌린다. 결과적으로 "보안을 위해 모든 세션을 종료했습니다" 라는 응답만 나가고 <b>세션은 그대로 살아 있었다.</b>
 *
 * <p>탈취된 토큰에 대한 대응이 실제로는 아무 일도 하지 않고 있었던 셈이다.
 *
 * <p>{@code REQUIRES_NEW} 로 독립 트랜잭션에서 커밋한다. 같은 클래스 안의 메서드로 두면 자기 호출이라 전파 설정이 적용되지 않으므로 클래스를 분리한다.
 */
@Component
public class SessionRevoker {

  private static final Logger log = LoggerFactory.getLogger(SessionRevoker.class);

  private final UserSessionRepository sessionRepository;

  public SessionRevoker(UserSessionRepository sessionRepository) {
    this.sessionRepository = sessionRepository;
  }

  /** 해당 사용자의 활성 세션을 모두 끊는다. 호출한 트랜잭션이 실패해도 이 커밋은 남는다. */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public int revokeAllForUser(Long userId) {
    int revoked = sessionRepository.revokeAllActive(userId, LocalDateTime.now());
    if (revoked > 0) {
      log.warn("사용자 세션 강제 종료 userId={} count={}", userId, revoked);
    }
    return revoked;
  }
}
