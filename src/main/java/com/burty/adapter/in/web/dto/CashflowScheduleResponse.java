package com.burty.adapter.in.web.dto;

public class CashflowScheduleResponse {
    private String scheduleId;
    private String scheduleTypeCode;
    private String label;
    private long amount;
    private String direction;
    private int dayOfMonth;
    private boolean active;

    public CashflowScheduleResponse() {}

    public CashflowScheduleResponse(String scheduleId, String scheduleTypeCode, String label, long amount,
                                    String direction, int dayOfMonth, boolean active) {
        this.scheduleId = scheduleId;
        this.scheduleTypeCode = scheduleTypeCode;
        this.label = label;
        this.amount = amount;
        this.direction = direction;
        this.dayOfMonth = dayOfMonth;
        this.active = active;
    }

    public String getScheduleId() { return scheduleId; }
    public void setScheduleId(String scheduleId) { this.scheduleId = scheduleId; }
    public String getScheduleTypeCode() { return scheduleTypeCode; }
    public void setScheduleTypeCode(String scheduleTypeCode) { this.scheduleTypeCode = scheduleTypeCode; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public long getAmount() { return amount; }
    public void setAmount(long amount) { this.amount = amount; }
    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }
    public int getDayOfMonth() { return dayOfMonth; }
    public void setDayOfMonth(int dayOfMonth) { this.dayOfMonth = dayOfMonth; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
