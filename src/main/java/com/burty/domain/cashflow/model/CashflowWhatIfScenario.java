package com.burty.domain.cashflow.model;

/** What-if 시뮬레이션 결과. */
public record CashflowWhatIfScenario(
    CashflowForecast baseline,
    CashflowForecast adjusted,
    long minimumBalanceDelta,
    String scenarioLabel) {}
