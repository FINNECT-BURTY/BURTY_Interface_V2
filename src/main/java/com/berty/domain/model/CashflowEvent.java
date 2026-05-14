package com.berty.domain.model;

import java.time.LocalDate;

public class CashflowEvent {
    private final LocalDate eventDate;
    private final long amount;
    private final String type;
    private final String category;
    private final String memo;

    public CashflowEvent(LocalDate eventDate, long amount, String type, String category, String memo) {
        this.eventDate = eventDate;
        this.amount = amount;
        this.type = type;
        this.category = category;
        this.memo = memo;
    }

    public LocalDate getEventDate() { return eventDate; }
    public long getAmount() { return amount; }
    public String getType() { return type; }
    public String getCategory() { return category; }
    public String getMemo() { return memo; }
}
