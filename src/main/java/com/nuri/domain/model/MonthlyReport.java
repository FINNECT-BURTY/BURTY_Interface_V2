package com.nuri.domain.model;

import java.util.List;

public class MonthlyReport {
    private String userId;
    private String month;
    private String easyReadSummary;
    private String signalColor;
    private String primaryAction;
    private List<String> keyPoints;

    public MonthlyReport(String userId, String month, String easyReadSummary, String signalColor, String primaryAction, List<String> keyPoints) {
        this.userId = userId;
        this.month = month;
        this.easyReadSummary = easyReadSummary;
        this.signalColor = signalColor;
        this.primaryAction = primaryAction;
        this.keyPoints = keyPoints;
    }

    public String getUserId() { return userId; }
    public String getMonth() { return month; }
    public String getEasyReadSummary() { return easyReadSummary; }
    public String getSignalColor() { return signalColor; }
    public String getPrimaryAction() { return primaryAction; }
    public List<String> getKeyPoints() { return keyPoints; }
}
