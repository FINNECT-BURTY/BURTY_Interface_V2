package com.burty.domain.repository;

import com.burty.domain.entity.ActionFeedbackScoreEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ActionFeedbackScoreRepository extends JpaRepository<ActionFeedbackScoreEntity, String> {
    Optional<ActionFeedbackScoreEntity> findByUserIdAndActionTypeCode(String userId, String actionTypeCode);

    List<ActionFeedbackScoreEntity> findByUserId(String userId);
}
