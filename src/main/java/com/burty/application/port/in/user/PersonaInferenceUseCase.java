/**
 *
 *
 * <pre>
 * <b>Description  : 사용자 유스케이스 포트 (PersonaInferenceUseCase)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.application.port.in.user
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
package com.burty.application.port.in.user;

import com.burty.domain.user.entity.PersonaProfileEntity;

public interface PersonaInferenceUseCase {

  PersonaProfileEntity getOrInfer(String userId);

  PersonaProfileEntity overrideByUser(
      String userId,
      String occupationCode,
      String residenceCode,
      String householdType,
      Long monthlyIncomeAvg);

  PersonaProfileEntity reinfer(String userId);
}
