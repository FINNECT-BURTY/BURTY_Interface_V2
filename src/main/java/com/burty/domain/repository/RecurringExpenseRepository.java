package com.burty.domain.repository;

import com.burty.domain.entity.RecurringExpenseEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecurringExpenseRepository extends JpaRepository<RecurringExpenseEntity, Long> {
    List<RecurringExpenseEntity> findByUserIdAndActiveTrue(Long userId);

    List<RecurringExpenseEntity> findByUserIdAndExpenseCategoryCodeAndActiveTrue(Long userId, String expenseCategoryCode);
}
