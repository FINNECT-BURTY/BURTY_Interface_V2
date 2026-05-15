package com.burty.adapter.in.web.dto;

import java.time.LocalDate;
import java.util.List;

public class CashflowCalendarDayResponse {
    private LocalDate date;
    private long expectedBalance;
    private boolean riskDay;
    private List<String> events;

    public CashflowCalendarDayResponse() {}

    public CashflowCalendarDayResponse(LocalDate date, long expectedBalance, boolean riskDay, List<String> events) {
        this.date = date;
        this.expectedBalance = expectedBalance;
        this.riskDay = riskDay;
        this.events = events;
    }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public long getExpectedBalance() { return expectedBalance; }
    public void setExpectedBalance(long expectedBalance) { this.expectedBalance = expectedBalance; }
    public boolean isRiskDay() { return riskDay; }
    public void setRiskDay(boolean riskDay) { this.riskDay = riskDay; }
    public List<String> getEvents() { return events; }
    public void setEvents(List<String> events) { this.events = events; }
}
