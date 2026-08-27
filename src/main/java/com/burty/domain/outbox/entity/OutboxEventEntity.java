package com.burty.domain.outbox.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 트랜잭셔널 아웃박스.
 *
 * <p>비즈니스 상태 변경과 "그 사실을 외부에 알리는 일"을 <b>같은 DB 트랜잭션</b>으로 묶기 위한 테이블이다. 이체 완료·알림 발송 같은 부수효과를 커밋 시점에 직접
 * 수행하면, 커밋은 됐는데 발송이 실패하거나 (유실) 발송은 됐는데 롤백되는 (유령 알림) dual-write 문제가 생긴다. 대신 여기에 한 줄을 같이 INSERT 하고,
 * {@code OutboxRelay} 가 커밋 이후 비동기로 꺼내 발송한다.
 */
@Entity
@Table(
    name = "tbl_outbox_event",
    indexes = {
      @Index(name = "idx_outbox_dispatch", columnList = "status, next_attempt_at"),
      @Index(name = "idx_outbox_aggregate", columnList = "aggregate_type, aggregate_id")
    })
@Getter
@Setter
@NoArgsConstructor
public class OutboxEventEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "event_id")
  private Long eventId;

  @Column(name = "aggregate_type", nullable = false, length = 64)
  private String aggregateType;

  @Column(name = "aggregate_id", nullable = false, length = 64)
  private String aggregateId;

  @Column(name = "event_type", nullable = false, length = 64)
  private String eventType;

  @Lob
  @Column(name = "payload", nullable = false)
  private String payload;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private Status status = Status.PENDING;

  @Column(name = "attempts", nullable = false)
  private Integer attempts = 0;

  @Column(name = "next_attempt_at", nullable = false)
  private LocalDateTime nextAttemptAt;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @Column(name = "published_at")
  private LocalDateTime publishedAt;

  @Column(name = "last_error", length = 500)
  private String lastError;

  public enum Status {
    /** 발행 대기 (릴레이가 집어감). */
    PENDING,
    /** 발행 성공. */
    PUBLISHED,
    /** 재시도 소진 — 사람이 봐야 함 (DLQ 역할). */
    DEAD
  }
}
