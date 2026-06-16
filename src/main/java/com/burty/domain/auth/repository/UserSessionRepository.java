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

public interface UserSessionRepository extends JpaRepository<UserSessionEntity, Long> {
  List<UserSessionEntity> findByUserIdAndRevokedAtIsNull(Long userId);

  Optional<UserSessionEntity> findByRefreshTokenHashAndRevokedAtIsNull(String refreshTokenHash);

  // 재사용 탐지용: revoked 여부 무관하게 hash 로 조회.
  // rotate() 가 이미 revoke 된 token 으로 들어오면 도난 의심으로 전체 세션 강제 종료.
  Optional<UserSessionEntity> findByRefreshTokenHash(String refreshTokenHash);
}
