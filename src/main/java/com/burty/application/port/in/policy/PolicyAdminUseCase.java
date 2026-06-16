/**
 *
 *
 * <pre>
 * <b>Description  : 정책 유스케이스 포트 (PolicyAdminUseCase)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.application.port.in.policy
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
package com.burty.application.port.in.policy;

import com.burty.application.dto.policy.PolicyAdminRequest;
import com.burty.application.dto.policy.PolicyAdminResponse;
import java.util.List;

public interface PolicyAdminUseCase {

  List<PolicyAdminResponse> listPolicies();

  PolicyAdminResponse upsert(PolicyAdminRequest request);

  void deactivate(String policyCode);
}
