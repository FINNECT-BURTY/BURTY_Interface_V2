package com.burty.adapter.in.web.dto;

public class AiFallbackTemplateRequest {
    private String templateKey;
    private String riskLevel;
    private String occupationCode;
    private String causeType;
    private String templateText;
    private Boolean active;

    public String getTemplateKey() { return templateKey; }
    public void setTemplateKey(String templateKey) { this.templateKey = templateKey; }
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    public String getOccupationCode() { return occupationCode; }
    public void setOccupationCode(String occupationCode) { this.occupationCode = occupationCode; }
    public String getCauseType() { return causeType; }
    public void setCauseType(String causeType) { this.causeType = causeType; }
    public String getTemplateText() { return templateText; }
    public void setTemplateText(String templateText) { this.templateText = templateText; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}
