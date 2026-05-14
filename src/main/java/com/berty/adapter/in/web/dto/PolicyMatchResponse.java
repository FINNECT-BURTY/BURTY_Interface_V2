package com.berty.adapter.in.web.dto;

public class PolicyMatchResponse {
    private String policyId;
    private String policyName;
    private String supportType;
    private String reason;
    private int priorityScore;

    public PolicyMatchResponse() {}

    public PolicyMatchResponse(String policyId, String policyName, String supportType, String reason, int priorityScore) {
        this.policyId = policyId;
        this.policyName = policyName;
        this.supportType = supportType;
        this.reason = reason;
        this.priorityScore = priorityScore;
    }

    public String getPolicyId() { return policyId; }
    public void setPolicyId(String policyId) { this.policyId = policyId; }
    public String getPolicyName() { return policyName; }
    public void setPolicyName(String policyName) { this.policyName = policyName; }
    public String getSupportType() { return supportType; }
    public void setSupportType(String supportType) { this.supportType = supportType; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public int getPriorityScore() { return priorityScore; }
    public void setPriorityScore(int priorityScore) { this.priorityScore = priorityScore; }
}
