package com.berty.domain.repository;

import com.berty.domain.entity.ActionFeedbackEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActionFeedbackRepository extends JpaRepository<ActionFeedbackEntity, Long> {
    long countByUserIdAndFeedbackIgnoreCase(String userId, String feedback);
    long countByUserIdAndActionTypeAndFeedbackIgnoreCase(String userId, String actionType, String feedback);
}
