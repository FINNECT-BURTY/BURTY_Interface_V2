package com.burty.domain.repository;

import com.burty.domain.entity.RiskAssessmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RiskAssessmentHistoryRepository extends JpaRepository<RiskAssessmentEntity, Long> {
    List<RiskAssessmentEntity> findTop30ByUserIdOrderByAssessedAtDesc(String userId);

    long countByUserIdAndLevel(String userId, String level);
}
