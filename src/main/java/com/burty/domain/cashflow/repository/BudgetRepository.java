package com.burty.domain.cashflow.repository;

import com.burty.domain.cashflow.entity.BudgetEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BudgetRepository extends JpaRepository<BudgetEntity, Long> {

  List<BudgetEntity> findByUserIdAndActiveTrue(String userId);

  List<BudgetEntity> findByUserId(String userId);

  Optional<BudgetEntity> findByUserIdAndCategoryCodeAndPeriodType(
      String userId, String categoryCode, BudgetEntity.PeriodType periodType);

  Optional<BudgetEntity> findByBudgetIdAndUserId(Long budgetId, String userId);
}
