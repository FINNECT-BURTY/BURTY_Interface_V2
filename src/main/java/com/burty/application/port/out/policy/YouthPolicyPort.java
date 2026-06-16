/**
 *
 *
 * <pre>
 * <b>Description  : 정책 포트 인터페이스 (YouthPolicyPort)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.application.port.out.policy
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
package com.burty.application.port.out.policy;

import com.burty.domain.policy.entity.YouthPolicyEntity;
import java.util.List;

public interface YouthPolicyPort {

  List<YouthPolicyEntity> fetchPolicies(
      int pageNum, int pageSize, String zipCd, String lclsfNm, String keyword);
}
