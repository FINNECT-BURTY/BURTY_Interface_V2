package com.burty.adapter.in.web.dto;

import java.util.List;

public class MonthlyReportResponse {
    private String userId;
    private String month;
    private String easyReadSummary;
    private String signalColor;
    private String primaryAction;
    private List<String> keyPoints;

    public MonthlyReportResponse() {}
    public MonthlyReportResponse(String userId, String month, String easyReadSummary, String signalColor, String primaryAction, List<String> keyPoints) {
        this.userId = userId;
        this.month = month;
        this.easyReadSummary = easyReadSummary;
        this.signalColor = signalColor;
        this.primaryAction = primaryAction;
        this.keyPoints = keyPoints;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getMonth() { return month; }
    public void setMonth(String month) { this.month = month; }
    public String getEasyReadSummary() { return easyReadSummary; }
    public void setEasyReadSummary(String easyReadSummary) { this.easyReadSummary = easyReadSummary; }
    public String getSignalColor() { return signalColor; }
    public void setSignalColor(String signalColor) { this.signalColor = signalColor; }
    public String getPrimaryAction() { return primaryAction; }
    public void setPrimaryAction(String primaryAction) { this.primaryAction = primaryAction; }
    public List<String> getKeyPoints() { return keyPoints; }
    public void setKeyPoints(List<String> keyPoints) { this.keyPoints = keyPoints; }
}
