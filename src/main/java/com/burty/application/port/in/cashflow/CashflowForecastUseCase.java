/**
 *
 *
 * <pre>
 * <b>Description  : 현금흐름 유스케이스 포트 (CashflowForecastUseCase)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.application.port.in.cashflow
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
package com.burty.application.port.in.cashflow;

import com.burty.domain.cashflow.model.CashflowCriteria;
import com.burty.domain.cashflow.model.CashflowForecast;
import com.burty.domain.cashflow.model.CashflowWhatIfScenario;

public interface CashflowForecastUseCase {

  CashflowForecast forecast(String userId);

  CashflowWhatIfScenario simulateWhatIf(
      String userId,
      String scenarioName,
      Long extraDailyExpense,
      Long incomeDelta,
      Integer expensePostponeDays);

  void updateCashflowCriteria(
      String userId, Long safetyBalance, Long openingBalanceOverride, Long monthlyVariableBudget);

  CashflowCriteria getCashflowCriteria(String userId);
}
