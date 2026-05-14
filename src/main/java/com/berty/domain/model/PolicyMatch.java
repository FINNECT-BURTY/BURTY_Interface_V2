package com.berty.domain.model;

public class PolicyMatch {
    private final String policyId;
    private final String policyName;
    private final String supportType;
    private final String reason;
    private final int priorityScore;

    public PolicyMatch(String policyId, String policyName, String supportType, String reason, int priorityScore) {
        this.policyId = policyId;
        this.policyName = policyName;
        this.supportType = supportType;
        this.reason = reason;
        this.priorityScore = priorityScore;
    }

    public String getPolicyId() { return policyId; }
    public String getPolicyName() { return policyName; }
    public String getSupportType() { return supportType; }
    public String getReason() { return reason; }
    public int getPriorityScore() { return priorityScore; }
}
