package com.nuri.application.port.out;

public interface MonthlyReportDeliveryPort {
    void deliver(String userId, byte[] pdfBytes, String fileName);
}
