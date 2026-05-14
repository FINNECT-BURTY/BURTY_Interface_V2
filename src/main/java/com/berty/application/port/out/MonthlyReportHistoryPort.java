package com.berty.application.port.out;

public interface MonthlyReportHistoryPort {
    void saveHistory(String userId, String month, String status, String detail);
}
