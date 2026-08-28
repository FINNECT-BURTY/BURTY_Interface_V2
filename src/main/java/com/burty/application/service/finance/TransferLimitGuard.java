package com.burty.application.service.finance;

import com.burty.core.constant.AppMessages;
import com.burty.core.error.enums.ErrorCode;
import com.burty.core.exception.BusinessException;
import com.burty.domain.finance.entity.DailyTransferUsageEntity;
import com.burty.domain.finance.repository.DailyTransferUsageRepository;
import com.burty.domain.user.entity.UserSettingEntity;
import com.burty.domain.user.repository.UserSettingRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 1일 이체 한도 예약/해제.
 *
 * <p>핵심은 <b>검사와 차감이 분리되면 안 된다</b>는 것이다. 예전 구현은 {@code assertWithinLimit()} 으로 읽어서 검사하고, 이체가 끝난 뒤에야
 * {@code recordUsage()} 로 더했다. 그 사이에 락이 없어서 동시 요청이 모두 같은 잔여한도를 보고 통과했다. 지금은 조건부 UPDATE 한 문장으로 예약하고,
 * 확정 실패했을 때만 되돌린다.
 *
 * <p>정합성 규칙: <b>결과가 UNKNOWN 인 이체는 해제하지 않는다.</b> 실제로 출금됐을 수 있으므로 한도를 소비한 것으로 간주하는 쪽이 안전하다. 정산 배치가
 * "은행에 없는 건"으로 확정하면 그때 해제한다.
 */
@Component
public class TransferLimitGuard {

  private static final Logger log = LoggerFactory.getLogger(TransferLimitGuard.class);

  private static final String DAILY_LIMIT_KEY = "TRANSFER_LIMIT";
  private static final String PER_TX_LIMIT_KEY = "TRANSFER_PER_TX_LIMIT";
  private static final long DEFAULT_DAILY_LIMIT = 5_000_000L;
  private static final long DEFAULT_PER_TX_LIMIT = 3_000_000L;

  /** 낙관적 잠금 충돌 재시도 횟수. 경합 대상이 (사용자, 날짜) 한 행뿐이라 이 정도면 충분하다. */
  private static final int MAX_CONFLICT_RETRIES = 12;

  private final UserSettingRepository userSettingRepository;
  private final DailyTransferUsageRepository dailyTransferUsageRepository;
  private final DailyTransferUsageInitializer usageInitializer;
  private final DailyTransferUsageWriter usageWriter;
  private final Clock clock;

  public TransferLimitGuard(
      UserSettingRepository userSettingRepository,
      DailyTransferUsageRepository dailyTransferUsageRepository,
      DailyTransferUsageInitializer usageInitializer,
      DailyTransferUsageWriter usageWriter,
      Clock clock) {
    this.userSettingRepository = userSettingRepository;
    this.dailyTransferUsageRepository = dailyTransferUsageRepository;
    this.usageInitializer = usageInitializer;
    this.usageWriter = usageWriter;
    this.clock = clock;
  }

  /**
   * 한도 내에서 사용량을 예약한다. 실패하면 {@link BusinessException}.
   *
   * <p><b>이 메서드에는 {@code @Transactional} 이 없다.</b> 낙관적 잠금 충돌이 나면 그 트랜잭션은 롤백되므로, 재시도는 반드시 트랜잭션 밖에서
   * 돌아야 한다. 실제 증감은 {@link DailyTransferUsageWriter} 가 독립 트랜잭션으로 수행한다.
   */
  public LocalDate reserve(String userId, long amount) {
    Long numericUserId = parseUserId(userId);
    long perTxLimit = resolvePerTransactionLimit(userId);
    if (amount > perTxLimit) {
      throw new BusinessException(
          ErrorCode.TRANSFER_LIMIT_EXCEEDED, AppMessages.Transfer.PER_TX_LIMIT_EXCEEDED);
    }

    long dailyLimit = resolveDailyLimit(userId);
    LocalDate today = LocalDate.now(clock);
    LocalDateTime now = LocalDateTime.now(clock);

    // 행이 없으면 만든다. 별도 트랜잭션이라 동시 생성 경쟁이 나도 이 호출 흐름은 오염되지 않는다.
    ensureUsageRow(numericUserId, userId, today, now);

    for (int attempt = 0; attempt < MAX_CONFLICT_RETRIES; attempt++) {
      try {
        if (usageWriter.tryReserve(numericUserId, today, amount, dailyLimit, now)) {
          // 실제로 차감한 날짜를 돌려준다. 호출자는 이 값을 주문에 기록해 두었다가 해제 시
          // 그대로 쓴다. 나중에 날짜를 다시 계산하면 자정을 걸친 이체에서 다른 행을 가리킨다.
          return today;
        }
        throw new BusinessException(
            ErrorCode.TRANSFER_LIMIT_EXCEEDED, AppMessages.Transfer.DAILY_LIMIT_EXCEEDED);
      } catch (OptimisticLockingFailureException e) {
        // 다른 요청이 먼저 갱신했다. 최신 값으로 다시 검사한다.
        log.debug("일일 한도 예약 충돌 — 재시도 {}/{} userId={}", attempt + 1, MAX_CONFLICT_RETRIES, userId);
      }
    }
    // 여기까지 왔다는 건 비정상적인 수준의 경합이다. 한도를 초과 통과시키느니 거절한다.
    log.warn("일일 한도 예약 재시도 소진 userId={} amount={}", userId, amount);
    throw new BusinessException(
        ErrorCode.TOO_MANY_REQUESTS, "이체 요청이 몰려 처리하지 못했습니다. 잠시 후 다시 시도해주세요.");
  }

  private void ensureUsageRow(
      Long numericUserId, String userId, LocalDate today, LocalDateTime now) {
    try {
      usageInitializer.ensureExists(numericUserId, today, now);
    } catch (RuntimeException e) {
      // 동시 생성 경쟁 시 내부 트랜잭션이 롤백되며 예외가 프록시 경계를 넘어 올라온다.
      // 오류가 아니라 "누군가 먼저 만들었다" 는 신호이므로 흡수한다.
      log.debug(
          "일일 사용량 행 생성 경쟁 — 기존 행으로 진행 userId={} date={} reason={}",
          userId,
          today,
          e.getClass().getSimpleName());
    }
  }

  /** 확정 실패한 이체의 예약을 되돌린다. UNKNOWN 상태에서는 호출하지 말 것 (실제로 출금됐을 수 있다). */
  public void release(String userId, long amount, LocalDate usageDate) {
    Long numericUserId = parseUserId(userId);
    for (int attempt = 0; attempt < MAX_CONFLICT_RETRIES; attempt++) {
      try {
        if (usageWriter.tryRelease(numericUserId, usageDate, amount, LocalDateTime.now(clock))) {
          return;
        }
        break;
      } catch (OptimisticLockingFailureException e) {
        log.debug("일일 한도 해제 충돌 — 재시도 {}/{} userId={}", attempt + 1, MAX_CONFLICT_RETRIES, userId);
      }
    }
    // 조용히 넘기면 사용자가 하루 종일 쓰지도 않은 한도에 묶인다.
    log.warn(
        "일일 이체 한도 해제 실패 — 수동 확인 필요 userId={} amount={} usageDate={}", userId, amount, usageDate);
  }

  public long resolveDailyLimit(String userId) {
    return resolveSetting(userId, DAILY_LIMIT_KEY, DEFAULT_DAILY_LIMIT);
  }

  public long resolvePerTransactionLimit(String userId) {
    return resolveSetting(userId, PER_TX_LIMIT_KEY, DEFAULT_PER_TX_LIMIT);
  }

  /** 오늘 사용한 누적 금액 (조회 전용). */
  @Transactional(readOnly = true)
  public long currentUsage(String userId) {
    return dailyTransferUsageRepository
        .findById_UserIdAndId_UsageDate(parseUserId(userId), LocalDate.now(clock))
        .map(DailyTransferUsageEntity::getTotalAmount)
        .orElse(0L);
  }

  private long resolveSetting(String userId, String key, long fallback) {
    long configured =
        userSettingRepository
            .findByUserIdAndSettingKey(userId, key)
            .map(UserSettingEntity::getSettingValueLong)
            .orElse(0L);
    return configured > 0 ? configured : fallback;
  }

  private static Long parseUserId(String userId) {
    try {
      return Long.parseLong(userId);
    } catch (NumberFormatException e) {
      throw new BusinessException(
          ErrorCode.INVALID_INPUT_VALUE, AppMessages.Transfer.INVALID_USER_ID);
    }
  }
}
