package com.berty.domain.repository;

import com.berty.domain.entity.CategoryRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRuleRepository extends JpaRepository<CategoryRuleEntity, String> {
    List<CategoryRuleEntity> findByActiveTrueOrderByPriorityDesc();
}
