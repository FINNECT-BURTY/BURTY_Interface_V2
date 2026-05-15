package com.burty.adapter.in.web.dto;

import java.time.LocalDate;

public class PolicyAdminResponse {
    private String policyCode;
    private String policyTypeCode;
    private String title;
    private String applyUrl;
    private LocalDate validTo;
    private boolean active;

    public PolicyAdminResponse() {}

    public PolicyAdminResponse(String policyCode, String policyTypeCode, String title, String applyUrl, LocalDate validTo, boolean active) {
        this.policyCode = policyCode;
        this.policyTypeCode = policyTypeCode;
        this.title = title;
        this.applyUrl = applyUrl;
        this.validTo = validTo;
        this.active = active;
    }

    public String getPolicyCode() { return policyCode; }
    public void setPolicyCode(String policyCode) { this.policyCode = policyCode; }
    public String getPolicyTypeCode() { return policyTypeCode; }
    public void setPolicyTypeCode(String policyTypeCode) { this.policyTypeCode = policyTypeCode; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getApplyUrl() { return applyUrl; }
    public void setApplyUrl(String applyUrl) { this.applyUrl = applyUrl; }
    public LocalDate getValidTo() { return validTo; }
    public void setValidTo(LocalDate validTo) { this.validTo = validTo; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
