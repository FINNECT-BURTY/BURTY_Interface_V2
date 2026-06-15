/**
 *
 *
 * <pre>
 * <b>Description  : 현금흐름 도메인 모델 (CashflowForecast)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.domain.cashflow.model
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
package com.burty.domain.cashflow.model;

import java.time.LocalDate;
import java.util.List;

public record CashflowForecast(
    String userId,
    LocalDate generatedDate,
    long openingBalance,
    List<DailyBalancePoint> dailyBalances,
    LocalDate riskDate,
    String riskReason,
    long minimumBalance,
    long safetyBalance,
    String dataSource,
    boolean customCriteriaUsed) {

  public CashflowForecast(
      String userId,
      LocalDate generatedDate,
      long openingBalance,
      List<DailyBalancePoint> dailyBalances,
      LocalDate riskDate,
      String riskReason,
      long minimumBalance) {
    this(
        userId,
        generatedDate,
        openingBalance,
        dailyBalances,
        riskDate,
        riskReason,
        minimumBalance,
        50_000L,
        "UNKNOWN",
        false);
  }
}
