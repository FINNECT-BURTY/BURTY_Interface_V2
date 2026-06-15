package com.burty.application.service.finance;

import com.burty.core.constant.AppMessages;
import com.burty.core.error.enums.ErrorCode;
import com.burty.core.exception.BusinessException;
import com.burty.domain.finance.entity.DailyTransferUsageEntity;
import com.burty.domain.finance.entity.DailyTransferUsageId;
import com.burty.domain.finance.repository.DailyTransferUsageRepository;
import com.burty.domain.user.entity.UserSettingEntity;
import com.burty.domain.user.repository.UserSettingRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 1일 이체 한도 검증 및 사용량 집계. */
@Component
@RequiredArgsConstructor
public class TransferLimitGuard {

  private static final String LIMIT_KEY = "TRANSFER_LIMIT";
  private static final long DEFAULT_DAILY_LIMIT = 5_000_000L;

  private final UserSettingRepository userSettingRepository;
  private final DailyTransferUsageRepository dailyTransferUsageRepository;

  public void assertWithinLimit(String userId, long amount) {
    long limit = resolveDailyLimit(userId);
    long used = currentUsage(parseUserId(userId));
    if (used + amount > limit) {
      throw new BusinessException(
          ErrorCode.INVALID_INPUT_VALUE, AppMessages.Transfer.DAILY_LIMIT_EXCEEDED);
    }
  }

  @Transactional
  public void recordUsage(String userId, long amount) {
    Long numericUserId = parseUserId(userId);
    LocalDate today = LocalDate.now();
    DailyTransferUsageEntity usage =
        dailyTransferUsageRepository
            .findById_UserIdAndId_UsageDate(numericUserId, today)
            .orElseGet(() -> newUsage(numericUserId, today));
    usage.setTotalAmount((usage.getTotalAmount() == null ? 0L : usage.getTotalAmount()) + amount);
    usage.setTransferCount((usage.getTransferCount() == null ? 0 : usage.getTransferCount()) + 1);
    usage.setUpdatedAt(LocalDateTime.now());
    dailyTransferUsageRepository.save(usage);
  }

  public long resolveDailyLimit(String userId) {
    long configured =
        userSettingRepository
            .findByUserIdAndSettingKey(userId, LIMIT_KEY)
            .map(UserSettingEntity::getSettingValueLong)
            .orElse(0L);
    return configured > 0 ? configured : DEFAULT_DAILY_LIMIT;
  }

  private long currentUsage(Long userId) {
    return dailyTransferUsageRepository
        .findById_UserIdAndId_UsageDate(userId, LocalDate.now())
        .map(DailyTransferUsageEntity::getTotalAmount)
        .orElse(0L);
  }

  private static DailyTransferUsageEntity newUsage(Long userId, LocalDate date) {
    DailyTransferUsageEntity entity = new DailyTransferUsageEntity();
    DailyTransferUsageId id = new DailyTransferUsageId();
    id.setUserId(userId);
    id.setUsageDate(date);
    entity.setId(id);
    entity.setTotalAmount(0L);
    entity.setTransferCount(0);
    entity.setUpdatedAt(LocalDateTime.now());
    return entity;
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
