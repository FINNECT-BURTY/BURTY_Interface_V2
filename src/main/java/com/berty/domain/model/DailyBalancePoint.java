package com.berty.domain.model;

import java.time.LocalDate;

public class DailyBalancePoint {
    private final LocalDate date;
    private final long balance;

    public DailyBalancePoint(LocalDate date, long balance) {
        this.date = date;
        this.balance = balance;
    }

    public LocalDate getDate() { return date; }
    public long getBalance() { return balance; }
}
