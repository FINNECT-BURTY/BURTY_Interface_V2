package com.burty.application.port.in;

import java.util.Map;

public interface KpiDashboardUseCase {
    Map<String, Object> userKpi(String userId);

    Map<String, Object> globalKpi();
}
