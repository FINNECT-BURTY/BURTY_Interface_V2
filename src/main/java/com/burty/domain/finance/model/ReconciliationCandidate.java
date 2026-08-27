package com.burty.domain.finance.model;

import java.time.LocalDateTime;

/**
 * 정산 대상 이체 건의 스냅샷.
 *
 * <p>엔티티 대신 이 프로젝션을 쓰는 이유: 정산은 은행 HTTP 호출을 트랜잭션 <b>밖에서</b> 해야 하는데, 엔티티를 들고 나가면 {@code user} 같은 LAZY
 * 연관 접근에서 {@code LazyInitializationException} 이 난다 ({@code open-in-view=false}). 필요한 값만 쿼리 시점에
 * 평탄화한다.
 */
public record ReconciliationCandidate(
    Long orderId,
    Long userId,
    String idempotencyKey,
    Long amount,
    LocalDateTime requestedAt,
    Integer attempts) {

  public String userIdAsString() {
    return String.valueOf(userId);
  }

  public int attemptsOrZero() {
    return attempts == null ? 0 : attempts;
  }
}
