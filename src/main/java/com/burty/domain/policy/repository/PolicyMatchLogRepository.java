/**
 *
 *
 * <pre>
 * <b>Description  : 정책 리포지토리 (PolicyMatchLogRepository)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.domain.policy.repository
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
package com.burty.domain.policy.repository;

import com.burty.domain.policy.entity.PolicyMatchLogEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PolicyMatchLogRepository extends JpaRepository<PolicyMatchLogEntity, Long> {
  List<PolicyMatchLogEntity> findTop20ByUserIdOrderByMatchedAtDesc(String userId);

  Optional<PolicyMatchLogEntity> findFirstByUserIdAndPolicyCodeOrderByMatchedAtDesc(
      String userId, String policyCode);

  long countByPolicyCode(String policyCode);

  long countByPolicyCodeAndAppliedTrue(String policyCode);
}
