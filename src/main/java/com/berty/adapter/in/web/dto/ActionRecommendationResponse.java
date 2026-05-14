package com.berty.adapter.in.web.dto;

public class ActionRecommendationResponse {
    private String actionType;
    private String title;
    private String description;
    private long estimatedImprovement;
    private double priorityScore;
    private String advisoryBoundary;

    public ActionRecommendationResponse() {}

    public ActionRecommendationResponse(String actionType, String title, String description, long estimatedImprovement, double priorityScore) {
        this(actionType, title, description, estimatedImprovement, priorityScore, null);
    }

    public ActionRecommendationResponse(String actionType, String title, String description, long estimatedImprovement,
                                        double priorityScore, String advisoryBoundary) {
        this.actionType = actionType;
        this.title = title;
        this.description = description;
        this.estimatedImprovement = estimatedImprovement;
        this.priorityScore = priorityScore;
        this.advisoryBoundary = advisoryBoundary;
    }

    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public long getEstimatedImprovement() { return estimatedImprovement; }
    public void setEstimatedImprovement(long estimatedImprovement) { this.estimatedImprovement = estimatedImprovement; }
    public double getPriorityScore() { return priorityScore; }
    public void setPriorityScore(double priorityScore) { this.priorityScore = priorityScore; }
    public String getAdvisoryBoundary() { return advisoryBoundary; }
    public void setAdvisoryBoundary(String advisoryBoundary) { this.advisoryBoundary = advisoryBoundary; }
}
