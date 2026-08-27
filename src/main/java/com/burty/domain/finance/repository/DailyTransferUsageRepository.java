package com.burty.domain.finance.repository;

import com.burty.domain.finance.entity.DailyTransferUsageEntity;
import com.burty.domain.finance.entity.DailyTransferUsageId;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DailyTransferUsageRepository
    extends JpaRepository<DailyTransferUsageEntity, DailyTransferUsageId> {

  Optional<DailyTransferUsageEntity> findById_UserIdAndId_UsageDate(
      Long userId, LocalDate usageDate);

  /**
   * 사용량 행을 <b>배타 락으로</b> 조회한다.
   *
   * <p>왜 조건부 UPDATE 만으로는 부족한가. InnoDB 는 UPDATE 의 WHERE 를 락 획득 후 최신 커밋 버전으로 재평가하지만, MVCC 스냅샷 기준으로
   * 평가하는 엔진에서는 두 트랜잭션이 같은 잔여한도를 보고 둘 다 통과할 수 있다. 실제로 동시성 테스트에서 한도 10건에 14건이 통과했다.
   *
   * <p>돈이 걸린 검사를 DB 엔진의 격리 수준 구현 차이에 맡길 수는 없다. 명시적 배타 락으로 직렬화한다. 대상은 (사용자, 날짜) 한 행이라 경합 범위가 좁아 비용도
   * 작다.
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      "select u from DailyTransferUsageEntity u"
          + " where u.id.userId = :userId and u.id.usageDate = :usageDate")
  Optional<DailyTransferUsageEntity> findForUpdate(
      @Param("userId") Long userId, @Param("usageDate") LocalDate usageDate);

  /**
   * 참고용 조건부 UPDATE. 엔진에 따라 원자성이 보장되지 않을 수 있으므로 한도 검사 경로에서는 사용하지 않는다.
   *
   * @return 갱신된 행 수. 0 이면 한도 초과 (또는 행 없음).
   */
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      "update DailyTransferUsageEntity u"
          + " set u.totalAmount = u.totalAmount + :amount,"
          + "     u.transferCount = u.transferCount + 1,"
          + "     u.updatedAt = :now"
          + " where u.id.userId = :userId and u.id.usageDate = :usageDate"
          + "   and u.totalAmount + :amount <= :limit")
  int reserveWithinLimit(
      @Param("userId") Long userId,
      @Param("usageDate") LocalDate usageDate,
      @Param("amount") long amount,
      @Param("limit") long limit,
      @Param("now") LocalDateTime now);

  /**
   * 예약한 사용량을 되돌린다. 이체가 확정 실패했을 때만 호출한다 (UNKNOWN 은 되돌리지 않는다 — 실제로 출금됐을 수 있으므로).
   *
   * <p>{@code GREATEST(0, ...)} 대신 조건절로 음수를 막는다.
   */
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      "update DailyTransferUsageEntity u"
          + " set u.totalAmount = u.totalAmount - :amount,"
          + "     u.transferCount = case when u.transferCount > 0 then u.transferCount - 1 else 0 end,"
          + "     u.updatedAt = :now"
          + " where u.id.userId = :userId and u.id.usageDate = :usageDate"
          + "   and u.totalAmount >= :amount")
  int release(
      @Param("userId") Long userId,
      @Param("usageDate") LocalDate usageDate,
      @Param("amount") long amount,
      @Param("now") LocalDateTime now);
}
