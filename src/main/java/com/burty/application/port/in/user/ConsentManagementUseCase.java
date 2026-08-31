/**
 *
 *
 * <pre>
 * <b>Description  : 사용자 유스케이스 포트 (ConsentManagementUseCase)</b>
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

import com.burty.application.dto.user.ConsentResponse;
import java.util.List;

public interface ConsentManagementUseCase {

  List<ConsentResponse> listConsents(String userId);

  /**
   * 동의를 철회한다.
   *
   * <p>{@code userId} 를 함께 받는다. 동의 ID 만으로 철회하면 남의 동의 이력도 철회할 수 있다. 동의는 규제 기록이라 남의 것을 건드리면 그 사용자의
   * 데이터 처리 근거가 사라지고 감사 추적도 어긋난다.
   */
  void revokeConsent(String userId, String consentId, String reason);

  void unlinkSocial(String userId, String provider);

  void revokeBiometric(String userId);
}
