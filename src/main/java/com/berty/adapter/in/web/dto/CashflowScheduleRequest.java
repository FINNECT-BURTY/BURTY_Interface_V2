package com.berty.adapter.in.web.dto;

public class CashflowScheduleRequest {
    private String userId;
    private String scheduleTypeCode;
    private String label;
    private Long amount;
    private String direction;
    private Integer dayOfMonth;

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getScheduleTypeCode() { return scheduleTypeCode; }
    public void setScheduleTypeCode(String scheduleTypeCode) { this.scheduleTypeCode = scheduleTypeCode; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public Long getAmount() { return amount; }
    public void setAmount(Long amount) { this.amount = amount; }
    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }
    public Integer getDayOfMonth() { return dayOfMonth; }
    public void setDayOfMonth(Integer dayOfMonth) { this.dayOfMonth = dayOfMonth; }
}
