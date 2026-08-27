package com.burty.domain.outbox.repository;

import com.burty.domain.outbox.entity.OutboxEventEntity;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, Long> {

  /**
   * 발행 대상 배치를 잠그고 가져온다.
   *
   * <p>{@code PESSIMISTIC_WRITE} + {@code SKIP LOCKED} 로 인스턴스가 여러 대여도 같은 이벤트를 두 번 집지 않는다. ({@code
   * SKIP LOCKED} 미지원 DB 에서는 대기하므로 동작은 유지되고 처리량만 떨어진다.)
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      "select e from OutboxEventEntity e"
          + " where e.status = :status and e.nextAttemptAt <= :now"
          + " order by e.eventId asc")
  List<OutboxEventEntity> lockPendingBatch(
      @Param("status") OutboxEventEntity.Status status,
      @Param("now") LocalDateTime now,
      Limit limit);

  /** 발행 대기 배치. enum 은 파라미터로 바인딩한다 (HQL 중첩 enum 리터럴은 매칭되지 않았다). */
  default List<OutboxEventEntity> lockPendingBatch(LocalDateTime now, Limit limit) {
    return lockPendingBatch(OutboxEventEntity.Status.PENDING, now, limit);
  }

  long countByStatus(OutboxEventEntity.Status status);

  /** 운영 조치용 — DEAD 등 특정 상태의 이벤트 목록. */
  List<OutboxEventEntity> findByStatusOrderByEventIdAsc(
      OutboxEventEntity.Status status, Limit limit);

  List<OutboxEventEntity> findByAggregateTypeAndAggregateIdOrderByEventIdAsc(
      String aggregateType, String aggregateId);
}
