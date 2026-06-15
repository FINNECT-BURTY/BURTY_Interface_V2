/**
 *
 *
 * <pre>
 * <b>Description  : 사용자 응답 DTO (PersonaResponse)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.application.dto.user
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
package com.burty.application.dto.user;

import com.burty.domain.user.entity.PersonaProfileEntity;
import java.time.LocalDateTime;

public record PersonaResponse(
    String userId,
    String occupationCode,
    String residenceCode,
    String householdType,
    Long monthlyIncomeAvg,
    Double incomeVariabilityPct,
    Integer age,
    String source,
    Boolean userOverridden,
    LocalDateTime inferredAt) {

  public static PersonaResponse from(PersonaProfileEntity entity) {
    return new PersonaResponse(
        entity.getUserId() == null ? null : entity.getUserId().toString(),
        entity.getOccupationCode(),
        entity.getResidenceCode(),
        entity.getHouseholdType(),
        entity.getMonthlyIncomeAvg(),
        entity.getIncomeVariabilityPct(),
        entity.getAge(),
        entity.getSource(),
        entity.getUserOverridden(),
        entity.getInferredAt());
  }
}
