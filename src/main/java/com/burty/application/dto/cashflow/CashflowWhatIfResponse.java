package com.burty.application.dto.cashflow;

import com.burty.domain.cashflow.model.CashflowForecast;
import com.burty.domain.cashflow.model.CashflowWhatIfScenario;

public record CashflowWhatIfResponse(
    CashflowForecastResponse baseline,
    CashflowForecastResponse adjusted,
    long minimumBalanceDelta,
    String scenarioLabel) {

  public static CashflowWhatIfResponse from(
      CashflowWhatIfScenario scenario,
      java.util.function.Function<CashflowForecast, CashflowForecastResponse> mapper) {
    return new CashflowWhatIfResponse(
        mapper.apply(scenario.baseline()),
        mapper.apply(scenario.adjusted()),
        scenario.minimumBalanceDelta(),
        scenario.scenarioLabel());
  }
}
