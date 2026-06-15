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

  void revokeConsent(String consentId, String reason);

  void unlinkSocial(String userId, String provider);

  void revokeBiometric(String userId);
}
