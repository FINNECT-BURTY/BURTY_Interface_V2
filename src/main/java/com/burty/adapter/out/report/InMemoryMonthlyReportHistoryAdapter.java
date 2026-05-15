package com.burty.adapter.out.report;

import com.burty.application.port.out.MonthlyReportHistoryPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnMissingBean(MonthlyReportHistoryPort.class)
public class InMemoryMonthlyReportHistoryAdapter implements MonthlyReportHistoryPort {
    @Override
    public void saveHistory(String userId, String month, String status, String detail) {
        log.info("Monthly report history user={} month={} status={} detail={}", userId, month, status, detail);
    }
}
