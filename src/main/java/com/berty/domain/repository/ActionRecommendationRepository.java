package com.berty.domain.repository;

import com.berty.domain.entity.ActionRecommendationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ActionRecommendationRepository extends JpaRepository<ActionRecommendationEntity, String> {
    List<ActionRecommendationEntity> findByActiveTrue();
}
