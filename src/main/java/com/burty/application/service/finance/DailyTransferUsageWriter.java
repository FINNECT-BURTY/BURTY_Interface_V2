package com.burty.application.service.finance;

import com.burty.domain.finance.entity.DailyTransferUsageEntity;
import com.burty.domain.finance.repository.DailyTransferUsageRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 일일 사용량의 실제 증감. 각 호출이 독립 트랜잭션이며 낙관적 잠금으로 보호된다.
 *
 * <p>재시도 루프를 여기 두지 않는 이유: 낙관적 잠금 충돌이 나면 그 트랜잭션은 이미 롤백 대상이다. 같은 트랜잭션 안에서 다시 시도해봐야 커밋되지 않는다. 재시도는 반드시
 * 트랜잭션 <b>밖</b>에서 (= 호출자인 {@link TransferLimitGuard} 에서) 돌아야 한다.
 */
@Component
public class DailyTransferUsageWriter {

  private final DailyTransferUsageRepository repository;

  public DailyTransferUsageWriter(DailyTransferUsageRepository repository) {
    this.repository = repository;
  }

  /**
   * @return 한도 내에서 예약에 성공하면 true, 한도 초과면 false
   * @throws org.springframework.dao.OptimisticLockingFailureException 동시 갱신 충돌 (호출자가 재시도)
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public boolean tryReserve(
      Long userId, LocalDate date, long amount, long limit, LocalDateTime now) {
    DailyTransferUsageEntity usage =
        repository.findById_UserIdAndId_UsageDate(userId, date).orElse(null);
    if (usage == null) {
      return false;
    }
    long used = usage.getTotalAmount() == null ? 0L : usage.getTotalAmount();
    if (used + amount > limit) {
      return false;
    }
    usage.setTotalAmount(used + amount);
    usage.setTransferCount((usage.getTransferCount() == null ? 0 : usage.getTransferCount()) + 1);
    usage.setUpdatedAt(now);
    repository.saveAndFlush(usage);
    return true;
  }

  /**
   * @return 해제에 성공하면 true
   * @throws org.springframework.dao.OptimisticLockingFailureException 동시 갱신 충돌 (호출자가 재시도)
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public boolean tryRelease(Long userId, LocalDate date, long amount, LocalDateTime now) {
    DailyTransferUsageEntity usage =
        repository.findById_UserIdAndId_UsageDate(userId, date).orElse(null);
    if (usage == null) {
      return false;
    }
    long used = usage.getTotalAmount() == null ? 0L : usage.getTotalAmount();
    if (used < amount) {
      return false;
    }
    usage.setTotalAmount(used - amount);
    usage.setTransferCount(
        usage.getTransferCount() == null || usage.getTransferCount() <= 0
            ? 0
            : usage.getTransferCount() - 1);
    usage.setUpdatedAt(now);
    repository.saveAndFlush(usage);
    return true;
  }
}
