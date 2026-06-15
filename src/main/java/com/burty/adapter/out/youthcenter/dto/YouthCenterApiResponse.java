/**
 *
 *
 * <pre>
 * <b>Description  : 외부연동 응답 DTO (YouthCenterApiResponse)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.adapter.out.youthcenter.dto
 * </pre>
 *
 * @author : RosieOh
 * @version : 1.0
 * @since
 *     <pre>
 * Modification Information
 *    수정일              수정자                수정내용
 * ---------------   ---------------   ----------------------------
 *  2026.06.15        RosieOh     최초생성
 *        </pre>
 */
package com.burty.adapter.out.youthcenter.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class YouthCenterApiResponse {

  @JsonProperty("youthPolicyList")
  private List<PolicyItem> youthPolicyList;

  public List<PolicyItem> getYouthPolicyList() {
    return youthPolicyList;
  }

  public void setYouthPolicyList(List<PolicyItem> list) {
    this.youthPolicyList = list;
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class PolicyItem {
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
    private String sprtTrgtAgeLmtYn;
    private String zipCd;
    private String earnCndSeCd;
    private String earnMinAmt;
    private String earnMaxAmt;
    private String earnEtcCn;
    private String mrgSttsCd;
    private String jobCd;
    private String schoolCd;
    private String sBizCd;
    private String aplyYmd;
    private String frstRegDt;
    private String lastMdfcnDt;
    private String inqCnt;

    public String getPlcyNo() {
      return plcyNo;
    }

    public void setPlcyNo(String v) {
      this.plcyNo = v;
    }

    public String getPlcyNm() {
      return plcyNm;
    }

    public void setPlcyNm(String v) {
      this.plcyNm = v;
    }

    public String getPlcyKywdNm() {
      return plcyKywdNm;
    }

    public void setPlcyKywdNm(String v) {
      this.plcyKywdNm = v;
    }

    public String getPlcyExplnCn() {
      return plcyExplnCn;
    }

    public void setPlcyExplnCn(String v) {
      this.plcyExplnCn = v;
    }

    public String getLclsfNm() {
      return lclsfNm;
    }

    public void setLclsfNm(String v) {
      this.lclsfNm = v;
    }

    public String getMclsfNm() {
      return mclsfNm;
    }

    public void setMclsfNm(String v) {
      this.mclsfNm = v;
    }

    public String getPlcySprtCn() {
      return plcySprtCn;
    }

    public void setPlcySprtCn(String v) {
      this.plcySprtCn = v;
    }

    public String getSprvsnInstCdNm() {
      return sprvsnInstCdNm;
    }

    public void setSprvsnInstCdNm(String v) {
      this.sprvsnInstCdNm = v;
    }

    public String getOperInstCdNm() {
      return operInstCdNm;
    }

    public void setOperInstCdNm(String v) {
      this.operInstCdNm = v;
    }

    public String getAplyPrdSeCd() {
      return aplyPrdSeCd;
    }

    public void setAplyPrdSeCd(String v) {
      this.aplyPrdSeCd = v;
    }

    public String getBizPrdBgngYmd() {
      return bizPrdBgngYmd;
    }

    public void setBizPrdBgngYmd(String v) {
      this.bizPrdBgngYmd = v;
    }

    public String getBizPrdEndYmd() {
      return bizPrdEndYmd;
    }

    public void setBizPrdEndYmd(String v) {
      this.bizPrdEndYmd = v;
    }

    public String getPlcyAplyMthdCn() {
      return plcyAplyMthdCn;
    }

    public void setPlcyAplyMthdCn(String v) {
      this.plcyAplyMthdCn = v;
    }

    public String getAplyUrlAddr() {
      return aplyUrlAddr;
    }

    public void setAplyUrlAddr(String v) {
      this.aplyUrlAddr = v;
    }

    public String getRefUrlAddr1() {
      return refUrlAddr1;
    }

    public void setRefUrlAddr1(String v) {
      this.refUrlAddr1 = v;
    }

    public String getSprtTrgtMinAge() {
      return sprtTrgtMinAge;
    }

    public void setSprtTrgtMinAge(String v) {
      this.sprtTrgtMinAge = v;
    }

    public String getSprtTrgtMaxAge() {
      return sprtTrgtMaxAge;
    }

    public void setSprtTrgtMaxAge(String v) {
      this.sprtTrgtMaxAge = v;
    }

    public String getSprtTrgtAgeLmtYn() {
      return sprtTrgtAgeLmtYn;
    }

    public void setSprtTrgtAgeLmtYn(String v) {
      this.sprtTrgtAgeLmtYn = v;
    }

    public String getZipCd() {
      return zipCd;
    }

    public void setZipCd(String v) {
      this.zipCd = v;
    }

    public String getEarnCndSeCd() {
      return earnCndSeCd;
    }

    public void setEarnCndSeCd(String v) {
      this.earnCndSeCd = v;
    }

    public String getEarnMinAmt() {
      return earnMinAmt;
    }

    public void setEarnMinAmt(String v) {
      this.earnMinAmt = v;
    }

    public String getEarnMaxAmt() {
      return earnMaxAmt;
    }

    public void setEarnMaxAmt(String v) {
      this.earnMaxAmt = v;
    }

    public String getEarnEtcCn() {
      return earnEtcCn;
    }

    public void setEarnEtcCn(String v) {
      this.earnEtcCn = v;
    }

    public String getMrgSttsCd() {
      return mrgSttsCd;
    }

    public void setMrgSttsCd(String v) {
      this.mrgSttsCd = v;
    }

    public String getJobCd() {
      return jobCd;
    }

    public void setJobCd(String v) {
      this.jobCd = v;
    }

    public String getSchoolCd() {
      return schoolCd;
    }

    public void setSchoolCd(String v) {
      this.schoolCd = v;
    }

    public String getSBizCd() {
      return sBizCd;
    }

    public void setSBizCd(String v) {
      this.sBizCd = v;
    }

    public String getAplyYmd() {
      return aplyYmd;
    }

    public void setAplyYmd(String v) {
      this.aplyYmd = v;
    }

    public String getFrstRegDt() {
      return frstRegDt;
    }

    public void setFrstRegDt(String v) {
      this.frstRegDt = v;
    }

    public String getLastMdfcnDt() {
      return lastMdfcnDt;
    }

    public void setLastMdfcnDt(String v) {
      this.lastMdfcnDt = v;
    }

    public String getInqCnt() {
      return inqCnt;
    }

    public void setInqCnt(String v) {
      this.inqCnt = v;
    }
  }
}
