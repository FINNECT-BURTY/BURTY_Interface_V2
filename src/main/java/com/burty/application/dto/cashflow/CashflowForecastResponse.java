/**
 *
 *
 * <pre>
 * <b>Description  : 현금흐름 응답 DTO (CashflowForecastResponse)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.application.dto.cashflow
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
package com.burty.application.dto.cashflow;

import com.burty.domain.cashflow.model.DailyBalancePoint;
import java.time.LocalDate;
import java.util.List;

public record CashflowForecastResponse(
    String userId,
    LocalDate generatedDate,
    long openingBalance,
    long minimumBalance,
    LocalDate riskDate,
    String riskReason,
    List<DailyBalancePoint> dailyBalances,
    long safetyBalance,
    String dataSource,
    boolean customCriteriaUsed) {}
