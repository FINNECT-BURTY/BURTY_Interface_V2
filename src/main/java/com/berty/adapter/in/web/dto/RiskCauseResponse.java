package com.berty.adapter.in.web.dto;

public class RiskCauseResponse {
    private String causeType;
    private String label;
    private long impactAmount;
    private String reason;

    public RiskCauseResponse() {}

    public RiskCauseResponse(String causeType, String label, long impactAmount, String reason) {
        this.causeType = causeType;
        this.label = label;
        this.impactAmount = impactAmount;
        this.reason = reason;
    }

    public String getCauseType() { return causeType; }
    public void setCauseType(String causeType) { this.causeType = causeType; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public long getImpactAmount() { return impactAmount; }
    public void setImpactAmount(long impactAmount) { this.impactAmount = impactAmount; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
