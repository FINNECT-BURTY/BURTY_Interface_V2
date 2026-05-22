package com.burty.adapter.in.web.dto;

import com.burty.domain.entity.YouthPolicyEntity;

public class YouthPolicyResponse {
    private String plcyNo;
    private String plcyNm;
    private String plcyKywdNm;
    private String plcyExplnCn;
    private String lclsfNm;
    private String mclsfNm;
    private String plcySprtCn;
    private String sprvsnInstCdNm;
    private String operInstCdNm;
    private String aplyPrdSeCd;
    private String bizPrdBgngYmd;
    private String bizPrdEndYmd;
    private String plcyAplyMthdCn;
    private String aplyUrlAddr;
    private String refUrlAddr1;
    private String sprtTrgtMinAge;
    private String sprtTrgtMaxAge;
    private String zipCd;
    private String earnCndSeCd;
    private String earnMinAmt;
    private String earnMaxAmt;
    private String mrgSttsCd;
    private String jobCd;
    private String schoolCd;
    private String aplyYmd;
    private String frstRegDt;
    private String lastMdfcnDt;

    public static YouthPolicyResponse from(YouthPolicyEntity e) {
        YouthPolicyResponse r = new YouthPolicyResponse();
        r.plcyNo = e.getPlcyNo();
        r.plcyNm = e.getPlcyNm();
        r.plcyKywdNm = e.getPlcyKywdNm();
        r.plcyExplnCn = e.getPlcyExplnCn();
        r.lclsfNm = e.getLclsfNm();
        r.mclsfNm = e.getMclsfNm();
        r.plcySprtCn = e.getPlcySprtCn();
        r.sprvsnInstCdNm = e.getSprvsnInstCdNm();
        r.operInstCdNm = e.getOperInstCdNm();
        r.aplyPrdSeCd = e.getAplyPrdSeCd();
        r.bizPrdBgngYmd = e.getBizPrdBgngYmd();
        r.bizPrdEndYmd = e.getBizPrdEndYmd();
        r.plcyAplyMthdCn = e.getPlcyAplyMthdCn();
        r.aplyUrlAddr = e.getAplyUrlAddr();
        r.refUrlAddr1 = e.getRefUrlAddr1();
        r.sprtTrgtMinAge = e.getSprtTrgtMinAge();
        r.sprtTrgtMaxAge = e.getSprtTrgtMaxAge();
        r.zipCd = e.getZipCd();
        r.earnCndSeCd = e.getEarnCndSeCd();
        r.earnMinAmt = e.getEarnMinAmt();
        r.earnMaxAmt = e.getEarnMaxAmt();
        r.mrgSttsCd = e.getMrgSttsCd();
        r.jobCd = e.getJobCd();
        r.schoolCd = e.getSchoolCd();
        r.aplyYmd = e.getAplyYmd();
        r.frstRegDt = e.getFrstRegDt();
        r.lastMdfcnDt = e.getLastMdfcnDt();
        return r;
    }

    public String getPlcyNo() { return plcyNo; }
    public String getPlcyNm() { return plcyNm; }
    public String getPlcyKywdNm() { return plcyKywdNm; }
    public String getPlcyExplnCn() { return plcyExplnCn; }
    public String getLclsfNm() { return lclsfNm; }
    public String getMclsfNm() { return mclsfNm; }
    public String getPlcySprtCn() { return plcySprtCn; }
    public String getSprvsnInstCdNm() { return sprvsnInstCdNm; }
    public String getOperInstCdNm() { return operInstCdNm; }
    public String getAplyPrdSeCd() { return aplyPrdSeCd; }
    public String getBizPrdBgngYmd() { return bizPrdBgngYmd; }
    public String getBizPrdEndYmd() { return bizPrdEndYmd; }
    public String getPlcyAplyMthdCn() { return plcyAplyMthdCn; }
    public String getAplyUrlAddr() { return aplyUrlAddr; }
    public String getRefUrlAddr1() { return refUrlAddr1; }
    public String getSprtTrgtMinAge() { return sprtTrgtMinAge; }
    public String getSprtTrgtMaxAge() { return sprtTrgtMaxAge; }
    public String getZipCd() { return zipCd; }
    public String getEarnCndSeCd() { return earnCndSeCd; }
    public String getEarnMinAmt() { return earnMinAmt; }
    public String getEarnMaxAmt() { return earnMaxAmt; }
    public String getMrgSttsCd() { return mrgSttsCd; }
    public String getJobCd() { return jobCd; }
    public String getSchoolCd() { return schoolCd; }
    public String getAplyYmd() { return aplyYmd; }
    public String getFrstRegDt() { return frstRegDt; }
    public String getLastMdfcnDt() { return lastMdfcnDt; }
}
