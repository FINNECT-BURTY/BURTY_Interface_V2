/**
 *
 *
 * <pre>
 * <b>Description  : 인증 유스케이스 포트 (LoginRiskUseCase)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.application.port.in.auth
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
package com.burty.application.port.in.auth;

import com.burty.application.dto.auth.LoginRiskEvaluateRequest;
import com.burty.application.dto.auth.LoginRiskEvaluateResponse;

public interface LoginRiskUseCase {

  /**
   * @param userId 인증 토큰에서 꺼낸 사용자. 요청 본문의 userId 는 신뢰하지 않는다.
   */
  LoginRiskEvaluateResponse evaluate(String userId, LoginRiskEvaluateRequest request);
}
