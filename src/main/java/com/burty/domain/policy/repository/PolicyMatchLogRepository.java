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
import org.springframework.data.jpa.repository.Query;

public interface PolicyMatchLogRepository extends JpaRepository<PolicyMatchLogEntity, Long> {
  List<PolicyMatchLogEntity> findTop20ByUserIdOrderByMatchedAtDesc(String userId);

  Optional<PolicyMatchLogEntity> findFirstByUserIdAndPolicyCodeOrderByMatchedAtDesc(
      String userId, String policyCode);

  long countByPolicyCode(String policyCode);

  long countByPolicyCodeAndAppliedTrue(String policyCode);

  /**
   * 정책별 매칭·적용 건수를 한 번에 집계한다.
   *
   * <p>정책마다 count 쿼리를 두 번씩 돌리면 활성 정책 수만큼 왕복이 늘어난다(N+1).
   *
   * @return {@code [policyCode, matched, applied]} 배열의 목록
   */
  @Query(
      "select l.policyCode, count(l), sum(case when l.applied = true then 1 else 0 end)"
          + " from PolicyMatchLogEntity l group by l.policyCode")
  List<Object[]> aggregateByPolicyCode();
}
