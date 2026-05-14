package com.berty.adapter.in.web.dto;

import java.time.LocalDate;

public class PolicyAdminRequest {
    private String policyCode;
    private String policyTypeCode;
    private String title;
    private String supportType;
    private Integer ageMin;
    private Integer ageMax;
    private Long incomeMax;
    private String occupationCode;
    private String residenceCode;
    private String benefitSummary;
    private String applyUrl;
    private LocalDate validFrom;
    private LocalDate validTo;
    private Boolean active;
    private Integer priorityBase;

    public String getPolicyCode() { return policyCode; }
    public void setPolicyCode(String policyCode) { this.policyCode = policyCode; }
    public String getPolicyTypeCode() { return policyTypeCode; }
    public void setPolicyTypeCode(String policyTypeCode) { this.policyTypeCode = policyTypeCode; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSupportType() { return supportType; }
    public void setSupportType(String supportType) { this.supportType = supportType; }
    public Integer getAgeMin() { return ageMin; }
    public void setAgeMin(Integer ageMin) { this.ageMin = ageMin; }
    public Integer getAgeMax() { return ageMax; }
    public void setAgeMax(Integer ageMax) { this.ageMax = ageMax; }
    public Long getIncomeMax() { return incomeMax; }
    public void setIncomeMax(Long incomeMax) { this.incomeMax = incomeMax; }
    public String getOccupationCode() { return occupationCode; }
    public void setOccupationCode(String occupationCode) { this.occupationCode = occupationCode; }
    public String getResidenceCode() { return residenceCode; }
    public void setResidenceCode(String residenceCode) { this.residenceCode = residenceCode; }
    public String getBenefitSummary() { return benefitSummary; }
    public void setBenefitSummary(String benefitSummary) { this.benefitSummary = benefitSummary; }
    public String getApplyUrl() { return applyUrl; }
    public void setApplyUrl(String applyUrl) { this.applyUrl = applyUrl; }
    public LocalDate getValidFrom() { return validFrom; }
    public void setValidFrom(LocalDate validFrom) { this.validFrom = validFrom; }
    public LocalDate getValidTo() { return validTo; }
    public void setValidTo(LocalDate validTo) { this.validTo = validTo; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public Integer getPriorityBase() { return priorityBase; }
    public void setPriorityBase(Integer priorityBase) { this.priorityBase = priorityBase; }
}
