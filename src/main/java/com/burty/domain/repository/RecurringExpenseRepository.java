package com.burty.domain.repository;

import com.burty.domain.entity.RecurringExpenseEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RecurringExpenseRepository extends JpaRepository<RecurringExpenseEntity, UUID> {
    List<RecurringExpenseEntity> findByUserIdAndActiveTrue(UUID userId);

    List<RecurringExpenseEntity> findByUserIdAndExpenseCategoryCodeAndActiveTrue(UUID userId, String expenseCategoryCode);
}
