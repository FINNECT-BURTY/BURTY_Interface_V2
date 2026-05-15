package com.burty.application.port.in;

import com.burty.domain.model.CashflowForecast;

public interface CashflowForecastUseCase {
    CashflowForecast forecast(String userId);
}
