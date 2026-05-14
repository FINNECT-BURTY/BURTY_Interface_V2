package com.berty.domain.repository;

import com.berty.domain.entity.ActionExecutionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ActionExecutionRepository extends JpaRepository<ActionExecutionEntity, Long> {
    long countByUserId(String userId);
    long countByUserIdAndActionType(String userId, String actionType);
    List<ActionExecutionEntity> findTop5ByUserIdOrderByExecutedAtDesc(String userId);
}
