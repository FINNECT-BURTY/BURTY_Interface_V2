package com.burty.adapter.in.web.dto;

import java.util.List;

public class LoginRiskEvaluateResponse {
    private String riskLevel;
    private List<String> reasons;

    public LoginRiskEvaluateResponse() {}

    public LoginRiskEvaluateResponse(String riskLevel, List<String> reasons) {
        this.riskLevel = riskLevel;
        this.reasons = reasons;
    }

    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    public List<String> getReasons() { return reasons; }
    public void setReasons(List<String> reasons) { this.reasons = reasons; }
}
