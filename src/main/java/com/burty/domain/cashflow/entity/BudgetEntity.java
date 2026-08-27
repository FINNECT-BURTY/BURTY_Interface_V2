package com.burty.domain.cashflow.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 예산.
 *
 * <p>거래 자동 분류와 현금흐름 예측은 이미 있었는데 정작 <b>예산이 없었다.</b> "이번 달 식비를 30만원으로 잡고, 넘으면 알려줘" 가 가계 관리 앱의 가장 기본적인
 * 요구인데 그걸 표현할 데이터가 없었다.
 *
 * <p>{@code categoryCode} 가 null 이면 전체 지출 예산이다. 카테고리별 예산과 전체 예산을 동시에 둘 수 있다.
 */
@Entity
@Table(
    name = "tbl_budget",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_budget_user_category_period",
            columnNames = {"user_id", "category_code", "period_type"}),
    indexes = @Index(name = "idx_budget_user", columnList = "user_id, active"))
@Getter
@Setter
@NoArgsConstructor
public class BudgetEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "budget_id")
  private Long budgetId;

  @Column(name = "user_id", nullable = false, length = 64)
  private String userId;

  /** 지출 카테고리 코드. null 이면 전체 지출 예산. */
  @Column(name = "category_code", length = 40)
  private String categoryCode;

  @Enumerated(EnumType.STRING)
  @Column(name = "period_type", nullable = false, length = 20)
  private PeriodType periodType = PeriodType.MONTHLY;

  @Column(name = "amount", nullable = false)
  private Long amount;

  /**
   * 경고 임계치 (%). 예산의 이 비율을 넘으면 미리 알린다.
   *
   * <p>100% 를 넘고 나서 알리면 이미 늦다. 시니어 사용자에게는 특히 그렇다.
   */
  @Column(name = "alert_threshold_percent", nullable = false)
  private Integer alertThresholdPercent = 80;

  @Column(name = "active", nullable = false)
  private Boolean active = true;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  public enum PeriodType {
    MONTHLY,
    WEEKLY
  }

  /** 전체 지출 예산인가. */
  public boolean isTotalBudget() {
    return categoryCode == null || categoryCode.isBlank();
  }
}
