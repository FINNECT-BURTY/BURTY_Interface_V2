/**
 *
 *
 * <pre>
 * <b>Description  : 현금흐름 (CashflowUserCriteriaStore)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.application.service.cashflow
 * </pre>
 *
 * @author : RosieOh
 * @version : 1.0
 * @since
 *     <pre>
 * Modification Information
 *    수정일              수정자                수정내용
 * ---------------   ---------------   ----------------------------
 *  2026.06.15        RosieOh     최초생성
 *        </pre>
 */
package com.burty.application.service.cashflow;

import com.burty.core.error.enums.ErrorCode;
import com.burty.core.exception.BusinessException;
import com.burty.domain.cashflow.model.CashflowCriteria;
import com.burty.domain.user.entity.UserSettingEntity;
import com.burty.domain.user.repository.UserSettingRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CashflowUserCriteriaStore {

  public static final String SETTING_SAFETY_BALANCE = "SAFETY_BALANCE";
  public static final String SETTING_OPENING_BALANCE = "OPENING_BALANCE_OVERRIDE";
  public static final String SETTING_MONTHLY_VARIABLE_BUDGET = "MONTHLY_VARIABLE_BUDGET";

  private final UserSettingRepository userSettingRepository;

  public CashflowCriteria getCriteria(String userId) {
    return new CashflowCriteria(
        userId,
        settingLong(userId, SETTING_SAFETY_BALANCE),
        settingLong(userId, SETTING_OPENING_BALANCE),
        settingLong(userId, SETTING_MONTHLY_VARIABLE_BUDGET),
        List.of("USER_CUSTOM_CRITERIA", "MYDATA_SCHEDULES", "MYDATA_ASSET_FALLBACK"));
  }

  public void updateCriteria(
      String userId, Long safetyBalance, Long openingBalanceOverride, Long monthlyVariableBudget) {
    if (safetyBalance != null) {
      upsertLongSetting(userId, SETTING_SAFETY_BALANCE, nonNegative(safetyBalance, "안전잔액"));
    }
    if (openingBalanceOverride != null) {
      upsertLongSetting(
          userId, SETTING_OPENING_BALANCE, nonNegative(openingBalanceOverride, "현재잔액"));
    }
    if (monthlyVariableBudget != null) {
      upsertLongSetting(
          userId, SETTING_MONTHLY_VARIABLE_BUDGET, nonNegative(monthlyVariableBudget, "월 변동지출 예산"));
    }
  }

  public long settingLong(String userId, String key, long defaultValue) {
    Long value = settingLong(userId, key);
    return value == null ? defaultValue : value;
  }

  public Long settingLong(String userId, String key) {
    return userSettingRepository
        .findByUserIdAndSettingKey(userId, key)
        .map(
            it ->
                it.getSettingValueLong() != null
                    ? it.getSettingValueLong()
                    : parseNullableLong(it.getSettingValueStr()))
        .orElse(null);
  }

  private void upsertLongSetting(String userId, String key, long value) {
    UserSettingEntity setting =
        userSettingRepository
            .findByUserIdAndSettingKey(userId, key)
            .orElseGet(UserSettingEntity::new);
    setting.setUserId(userId);
    setting.setSettingKey(key);
    setting.setSettingValueLong(value);
    userSettingRepository.save(setting);
  }

  private long nonNegative(long value, String label) {
    if (value < 0) {
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, label + "은 0 이상이어야 합니다.");
    }
    return value;
  }

  private Long parseNullableLong(String value) {
    if (value == null || value.isBlank()) return null;
    try {
      return Long.parseLong(value);
    } catch (NumberFormatException ex) {
      return null;
    }
  }
}
