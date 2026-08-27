package com.burty.domain.cashflow.repository;

import com.burty.domain.cashflow.entity.BudgetAlertEntity;
import com.burty.domain.cashflow.entity.BudgetAlertEntity.Level;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BudgetAlertRepository extends JpaRepository<BudgetAlertEntity, Long> {

  boolean existsByBudgetIdAndPeriodKeyAndLevel(Long budgetId, String periodKey, Level level);

  List<BudgetAlertEntity> findByUserIdAndPeriodKeyOrderByAlertIdDesc(
      String userId, String periodKey);
}
