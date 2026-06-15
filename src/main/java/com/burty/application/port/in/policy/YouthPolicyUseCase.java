/**
 *
 *
 * <pre>
 * <b>Description  : 정책 유스케이스 포트 (YouthPolicyUseCase)</b>
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

import com.burty.domain.policy.entity.YouthPolicyEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface YouthPolicyUseCase {

  int syncPolicies(String zipCd, String lclsfNm, String keyword);

  Page<YouthPolicyEntity> searchPolicies(
      String lclsfNm,
      String mclsfNm,
      String zipCd,
      String keyword,
      Integer minAge,
      Integer maxAge,
      Pageable pageable);
}
