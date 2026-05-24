package com.burty.adapter.in.web.dto;

import com.burty.domain.entity.YouthPolicyEntity;

public class YouthPolicySummary {

    private String id;
    private String title;
    private String keywords;
    private String description;
    private String category;
    private String subCategory;
    private String supportContent;
    private String supervisingOrg;
    private String operatingOrg;
    private String startDate;
    private String endDate;
    private String applyMethod;
    private String applyUrl;
    private String referenceUrl;
    private String minAge;
    private String maxAge;
    private String targetRegions;
    private String incomeConditionCode;
    private String minIncome;
    private String maxIncome;
    private String incomeEtc;
    private String maritalStatusCode;
    private String jobCode;
    private String educationCode;
    private String applyDeadline;
    private String registeredAt;
    private String updatedAt;

    public static YouthPolicySummary from(YouthPolicyEntity e) {
        YouthPolicySummary r = new YouthPolicySummary();
        r.id              = e.getPlcyNo();
        r.title           = e.getPlcyNm();
        r.keywords        = e.getPlcyKywdNm();
        r.description     = e.getPlcyExplnCn();
        r.category        = e.getLclsfNm();
        r.subCategory     = e.getMclsfNm();
        r.supportContent  = e.getPlcySprtCn();
        r.supervisingOrg  = e.getSprvsnInstCdNm();
        r.operatingOrg    = e.getOperInstCdNm();
        r.startDate       = e.getBizPrdBgngYmd();
        r.endDate         = e.getBizPrdEndYmd();
        r.applyMethod     = e.getPlcyAplyMthdCn();
        r.applyUrl        = e.getAplyUrlAddr();
        r.referenceUrl    = e.getRefUrlAddr1();
        r.minAge          = e.getSprtTrgtMinAge();
        r.maxAge          = e.getSprtTrgtMaxAge();
        r.targetRegions   = e.getZipCd();
        r.incomeConditionCode = e.getEarnCndSeCd();
        r.minIncome       = e.getEarnMinAmt();
        r.maxIncome       = e.getEarnMaxAmt();
        r.incomeEtc       = e.getEarnEtcCn();
        r.maritalStatusCode = e.getMrgSttsCd();
        r.jobCode         = e.getJobCd();
        r.educationCode   = e.getSchoolCd();
        r.applyDeadline   = e.getAplyYmd();
        r.registeredAt    = e.getFrstRegDt();
        r.updatedAt       = e.getLastMdfcnDt();
        return r;
    }

    public String getId()               { return id; }
    public String getTitle()            { return title; }
    public String getKeywords()         { return keywords; }
    public String getDescription()      { return description; }
    public String getCategory()         { return category; }
    public String getSubCategory()      { return subCategory; }
    public String getSupportContent()   { return supportContent; }
    public String getSupervisingOrg()   { return supervisingOrg; }
    public String getOperatingOrg()     { return operatingOrg; }
    public String getStartDate()        { return startDate; }
    public String getEndDate()          { return endDate; }
    public String getApplyMethod()      { return applyMethod; }
    public String getApplyUrl()         { return applyUrl; }
    public String getReferenceUrl()     { return referenceUrl; }
    public String getMinAge()           { return minAge; }
    public String getMaxAge()           { return maxAge; }
    public String getTargetRegions()    { return targetRegions; }
    public String getIncomeConditionCode() { return incomeConditionCode; }
    public String getMinIncome()        { return minIncome; }
    public String getMaxIncome()        { return maxIncome; }
    public String getIncomeEtc()        { return incomeEtc; }
    public String getMaritalStatusCode() { return maritalStatusCode; }
    public String getJobCode()          { return jobCode; }
    public String getEducationCode()    { return educationCode; }
    public String getApplyDeadline()    { return applyDeadline; }
    public String getRegisteredAt()     { return registeredAt; }
    public String getUpdatedAt()        { return updatedAt; }
}
