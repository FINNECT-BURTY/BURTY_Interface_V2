package com.nuri.domain.model;

import java.util.List;

public class ConsultationResult {
    private String summary;
    private String signalColor;
    private List<String> recommendedActions;

    public ConsultationResult(String summary, String signalColor, List<String> recommendedActions) {
        this.summary = summary;
        this.signalColor = signalColor;
        this.recommendedActions = recommendedActions;
    }

    public String getSummary() { return summary; }
    public String getSignalColor() { return signalColor; }
    public List<String> getRecommendedActions() { return recommendedActions; }
}
