package com.burty.application.dto.cashflow;

public record CashflowWhatIfRequest(
    String userId,
    String scenarioName,
    Long extraDailyExpense,
    Long incomeDelta,
    Integer expensePostponeDays) {}
