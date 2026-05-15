package com.burty.adapter.in.web.dto;

import java.util.List;

public class ConsultResponse {
    private String summary;
    private String signalColor;
    private List<String> recommendedActions;

    public ConsultResponse() {}
    public ConsultResponse(String summary, String signalColor, List<String> recommendedActions) {
        this.summary = summary;
        this.signalColor = signalColor;
        this.recommendedActions = recommendedActions;
    }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getSignalColor() { return signalColor; }
    public void setSignalColor(String signalColor) { this.signalColor = signalColor; }
    public List<String> getRecommendedActions() { return recommendedActions; }
    public void setRecommendedActions(List<String> recommendedActions) { this.recommendedActions = recommendedActions; }
}
