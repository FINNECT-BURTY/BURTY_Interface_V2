package com.burty.domain.model;

public class ActionRecommendation {
    private final String actionType;
    private final String title;
    private final String description;
    private final long estimatedImprovement;
    private final double priorityScore;
    private final String advisoryBoundary;

    public ActionRecommendation(String actionType, String title, String description, long estimatedImprovement, double priorityScore) {
        this(actionType, title, description, estimatedImprovement, priorityScore,
                "조회와 분석 기반의 참고 제안이며, 금융상품 가입/대출 실행/상환 조건 변경은 사용자 확인과 금융기관 절차가 필요합니다.");
    }

    public ActionRecommendation(String actionType, String title, String description, long estimatedImprovement,
                                double priorityScore, String advisoryBoundary) {
        this.actionType = actionType;
        this.title = title;
        this.description = description;
        this.estimatedImprovement = estimatedImprovement;
        this.priorityScore = priorityScore;
        this.advisoryBoundary = advisoryBoundary;
    }

    public String getActionType() { return actionType; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public long getEstimatedImprovement() { return estimatedImprovement; }
    public double getPriorityScore() { return priorityScore; }
    public String getAdvisoryBoundary() { return advisoryBoundary; }
}
