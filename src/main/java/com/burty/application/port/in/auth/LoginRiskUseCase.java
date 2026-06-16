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

  LoginRiskEvaluateResponse evaluate(LoginRiskEvaluateRequest request);
}
