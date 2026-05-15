package com.burty.domain.repository;

import com.burty.domain.entity.PolicyMatchLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PolicyMatchLogRepository extends JpaRepository<PolicyMatchLogEntity, Long> {
    List<PolicyMatchLogEntity> findTop20ByUserIdOrderByMatchedAtDesc(String userId);

    Optional<PolicyMatchLogEntity> findFirstByUserIdAndPolicyCodeOrderByMatchedAtDesc(String userId, String policyCode);

    long countByPolicyCode(String policyCode);

    long countByPolicyCodeAndAppliedTrue(String policyCode);
}
