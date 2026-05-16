package com.burty.domain.repository;

import com.burty.domain.entity.UserSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserSessionRepository extends JpaRepository<UserSessionEntity, UUID> {
    List<UserSessionEntity> findByUserIdAndRevokedAtIsNull(String userId);
    Optional<UserSessionEntity> findByRefreshTokenHashAndRevokedAtIsNull(String refreshTokenHash);

    // 재사용 탐지용: revoked 여부 무관하게 hash 로 조회.
    // rotate() 가 이미 revoke 된 token 으로 들어오면 도난 의심으로 전체 세션 강제 종료.
    Optional<UserSessionEntity> findByRefreshTokenHash(String refreshTokenHash);
}
