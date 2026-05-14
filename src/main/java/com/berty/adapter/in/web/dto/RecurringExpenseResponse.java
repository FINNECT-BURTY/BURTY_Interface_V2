package com.berty.adapter.in.web.dto;

public class RecurringExpenseResponse {
    private String name;
    private long amount;
    private int dayOfMonth;

    public RecurringExpenseResponse() {}

    public RecurringExpenseResponse(String name, long amount, int dayOfMonth) {
        this.name = name;
        this.amount = amount;
        this.dayOfMonth = dayOfMonth;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public long getAmount() { return amount; }
    public void setAmount(long amount) { this.amount = amount; }
    public int getDayOfMonth() { return dayOfMonth; }
    public void setDayOfMonth(int dayOfMonth) { this.dayOfMonth = dayOfMonth; }
}
