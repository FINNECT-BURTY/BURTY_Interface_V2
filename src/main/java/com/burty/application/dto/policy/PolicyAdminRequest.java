/**
 *
 *
 * <pre>
 * <b>Description  : 정책 요청 DTO (PolicyAdminRequest)</b>
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

import java.time.LocalDate;

public record PolicyAdminRequest(
    String policyCode,
    String policyTypeCode,
    String title,
    String supportType,
    Integer ageMin,
    Integer ageMax,
    Long incomeMax,
    String occupationCode,
    String residenceCode,
    String benefitSummary,
    String applyUrl,
    LocalDate validFrom,
    LocalDate validTo,
    Boolean active,
    Integer priorityBase) {}
