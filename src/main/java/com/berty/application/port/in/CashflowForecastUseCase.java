package com.berty.application.port.in;

import com.berty.domain.model.CashflowForecast;

public interface CashflowForecastUseCase {
    CashflowForecast forecast(String userId);
}
