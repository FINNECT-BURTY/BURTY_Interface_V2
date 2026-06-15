/**
 *
 *
 * <pre>
 * <b>Description  : 정책 응답 DTO (YouthPolicyResponse)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.application.dto.policy
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
package com.burty.application.dto.policy;

import com.burty.domain.policy.entity.YouthPolicyEntity;

public record YouthPolicyResponse(
    String plcyNo,
    String plcyNm,
    String plcyKywdNm,
    String plcyExplnCn,
    String lclsfNm,
    String mclsfNm,
    String plcySprtCn,
    String sprvsnInstCdNm,
    String operInstCdNm,
    String aplyPrdSeCd,
    String bizPrdBgngYmd,
    String bizPrdEndYmd,
    String plcyAplyMthdCn,
    String aplyUrlAddr,
    String refUrlAddr1,
    String sprtTrgtMinAge,
    String sprtTrgtMaxAge,
    String zipCd,
    String earnCndSeCd,
    String earnMinAmt,
    String earnMaxAmt,
    String mrgSttsCd,
    String jobCd,
    String schoolCd,
    String aplyYmd,
    String frstRegDt,
    String lastMdfcnDt) {
  public static YouthPolicyResponse from(YouthPolicyEntity e) {
    return new YouthPolicyResponse(
        e.getPlcyNo(),
        e.getPlcyNm(),
        e.getPlcyKywdNm(),
        e.getPlcyExplnCn(),
        e.getLclsfNm(),
        e.getMclsfNm(),
        e.getPlcySprtCn(),
        e.getSprvsnInstCdNm(),
        e.getOperInstCdNm(),
        e.getAplyPrdSeCd(),
        e.getBizPrdBgngYmd(),
        e.getBizPrdEndYmd(),
        e.getPlcyAplyMthdCn(),
        e.getAplyUrlAddr(),
        e.getRefUrlAddr1(),
        e.getSprtTrgtMinAge(),
        e.getSprtTrgtMaxAge(),
        e.getZipCd(),
        e.getEarnCndSeCd(),
        e.getEarnMinAmt(),
        e.getEarnMaxAmt(),
        e.getMrgSttsCd(),
        e.getJobCd(),
        e.getSchoolCd(),
        e.getAplyYmd(),
        e.getFrstRegDt(),
        e.getLastMdfcnDt());
  }
}
