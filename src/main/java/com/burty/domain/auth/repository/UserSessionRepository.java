/**
 *
 *
 * <pre>
 * <b>Description  : 인증 리포지토리 (UserSessionRepository)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.domain.auth.repository
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
package com.burty.domain.auth.repository;

import com.burty.domain.auth.entity.UserSessionEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserSessionRepository extends JpaRepository<UserSessionEntity, Long> {
  List<UserSessionEntity> findByUserIdAndRevokedAtIsNull(Long userId);

  Optional<UserSessionEntity> findByRefreshTokenHashAndRevokedAtIsNull(String refreshTokenHash);

  // 재사용 탐지용: revoked 여부 무관하게 hash 로 조회.
  // rotate() 가 이미 revoke 된 token 으로 들어오면 도난 의심으로 전체 세션 강제 종료.
  Optional<UserSessionEntity> findByRefreshTokenHash(String refreshTokenHash);

  /**
   * 아직 살아 있는 세션만 revoke 하고, 실제로 바꾼 행 수를 돌려준다.
   *
   * <p>회전은 "읽어서 확인하고 쓰기" 로는 안전하지 않다. 같은 refresh token 으로 두 요청이 동시에 들어오면 둘 다 {@code revokedAt ==
   * null} 을 보고 통과해 각자 새 세션을 발급받는다. 재사용 탐지가 정확히 그 상황을 잡으라고 있는 것인데 그때 뚫린다.
   *
   * <p>조건부 UPDATE 로 한쪽만 성공하게 만든다. 0 이 돌아왔다는 것은 다른 요청이 먼저 회전시켰다는 뜻이고, 곧 같은 토큰이 두 번 쓰였다는 뜻이다.
   *
   * @return 갱신된 행 수 (0 이면 이미 다른 요청이 회전시켰다)
   */
  @Modifying(clearAutomatically = true)
  @Query(
      "update UserSessionEntity s set s.revokedAt = :now"
          + " where s.sessionId = :sessionId and s.revokedAt is null")
  int revokeIfActive(@Param("sessionId") Long sessionId, @Param("now") java.time.LocalDateTime now);

  /** 해당 사용자의 활성 세션을 한 번에 끊는다. */
  @Modifying(clearAutomatically = true)
  @Query(
      "update UserSessionEntity s set s.revokedAt = :now"
          + " where s.userId = :userId and s.revokedAt is null")
  int revokeAllActive(@Param("userId") Long userId, @Param("now") java.time.LocalDateTime now);
}
