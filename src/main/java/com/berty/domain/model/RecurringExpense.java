package com.berty.domain.model;

public class RecurringExpense {
    private final String name;
    private final long amount;
    private final int dayOfMonth;

    public RecurringExpense(String name, long amount, int dayOfMonth) {
        this.name = name;
        this.amount = amount;
        this.dayOfMonth = dayOfMonth;
    }

    public String getName() { return name; }
    public long getAmount() { return amount; }
    public int getDayOfMonth() { return dayOfMonth; }
}
