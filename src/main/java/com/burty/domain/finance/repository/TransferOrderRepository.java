package com.burty.domain.finance.repository;

import com.burty.domain.finance.entity.TransferOrderEntity;
import com.burty.domain.finance.model.ReconciliationCandidate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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
          + "   o.reconcileAttempts, o.limitUsageDate)"
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

  /** 건수만 필요할 때. 전체를 적재해 size() 를 세지 않기 위함이다. */
  long countByUser_UserId(Long userId);

  /**
   * 아직 확정되지 않은 주문만 종결 상태로 바꾸고, 실제로 바꾼 행 수를 돌려준다.
   *
   * <p>확정은 <b>한 번만</b> 일어나야 한다. 정산 배치와 관리자 수동 확정이 같은 주문을 동시에 집으면, 조회 후 상태 검사로는 둘 다 통과해 각자 확정하고
   * <b>각자 한도를 되돌린다.</b> 차감은 한 번인데 복구가 두 번이면 그만큼 한도가 늘어난다.
   *
   * @return 1 이면 이 호출이 확정했다, 0 이면 이미 다른 쪽이 확정했다
   */
  @Modifying(clearAutomatically = true)
  @Query(
      "update TransferOrderEntity o set o.status = :status, o.nextReconcileAt = null"
          + " where o.orderId = :orderId"
          + " and o.status not in (com.burty.domain.finance.entity.TransferOrderEntity.Status.EXECUTED,"
          + " com.burty.domain.finance.entity.TransferOrderEntity.Status.FAILED,"
          + " com.burty.domain.finance.entity.TransferOrderEntity.Status.CANCELLED,"
          + " com.burty.domain.finance.entity.TransferOrderEntity.Status.REVERSED)")
  int settleIfNotTerminal(
      @Param("orderId") Long orderId, @Param("status") TransferOrderEntity.Status status);
}
