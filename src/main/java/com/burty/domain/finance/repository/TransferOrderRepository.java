package com.burty.domain.finance.repository;

import com.burty.domain.finance.entity.TransferOrderEntity;
import com.burty.domain.finance.model.ReconciliationCandidate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransferOrderRepository extends JpaRepository<TransferOrderEntity, Long> {
  List<TransferOrderEntity> findByUser_UserId(Long userId);

  List<TransferOrderEntity> findByUser_UserIdOrderByOrderIdDesc(Long userId);

  Optional<TransferOrderEntity> findByIdempotencyKey(String idempotencyKey);

  Optional<TransferOrderEntity> findByUser_UserIdAndIdempotencyKey(
      Long userId, String idempotencyKey);

  // 소유자 검증에서 user 를 즉시 참조하므로 함께 가져온다 (LAZY 추가 쿼리 방지).
  @EntityGraph(attributePaths = "user")
  Optional<TransferOrderEntity> findByBankTransactionId(String bankTransactionId);

  @EntityGraph(attributePaths = "user")
  Optional<TransferOrderEntity> findWithUserByOrderId(Long orderId);

  /**
   * 정산 대상 조회.
   *
   * <ul>
   *   <li>{@code UNKNOWN} — 은행 응답을 못 받은 건. 예정 시각이 지난 것만.
   *   <li>{@code EXECUTING} — 은행에 요청을 보낸 뒤 프로세스가 죽어 상태가 멈춘 건.
   * </ul>
   */
  @Query(
      "select new com.burty.domain.finance.model.ReconciliationCandidate("
          + "   o.orderId, o.user.userId, o.idempotencyKey, o.amount, o.requestedAt,"
          + "   o.reconcileAttempts)"
          + " from TransferOrderEntity o"
          + " where (o.status = :unknownStatus"
          + "        and o.nextReconcileAt is not null and o.nextReconcileAt <= :now)"
          + "    or (o.status = :executingStatus and o.requestedAt <= :stuckBefore)"
          + " order by o.orderId asc")
  List<ReconciliationCandidate> findReconciliationCandidates(
      @Param("unknownStatus") TransferOrderEntity.Status unknownStatus,
      @Param("executingStatus") TransferOrderEntity.Status executingStatus,
      @Param("now") LocalDateTime now,
      @Param("stuckBefore") LocalDateTime stuckBefore,
      Limit limit);

  /** 정산 대상 조회. enum 은 파라미터로 바인딩한다 (HQL 중첩 enum 리터럴은 매칭되지 않았다). */
  default List<ReconciliationCandidate> findReconciliationCandidates(
      LocalDateTime now, LocalDateTime stuckBefore, Limit limit) {
    return findReconciliationCandidates(
        TransferOrderEntity.Status.UNKNOWN,
        TransferOrderEntity.Status.EXECUTING,
        now,
        stuckBefore,
        limit);
  }

  long countByStatus(TransferOrderEntity.Status status);
}
