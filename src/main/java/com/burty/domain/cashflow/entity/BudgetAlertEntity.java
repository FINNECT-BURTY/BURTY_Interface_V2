package com.burty.domain.cashflow.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 예산 경고 발송 이력.
 *
 * <p>이 테이블의 존재 이유는 <b>중복 알림 방지</b>다. 예산 초과 상태는 한 번 발생하면 그 달 내내 유지되므로, 거래가 들어올 때마다 평가하면 같은 경고가 수십 번
 * 나간다. (기간, 예산, 단계) 조합에 유니크를 걸어 한 번만 나가게 한다.
 */
@Entity
@Table(
    name = "tbl_budget_alert",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_budget_alert_period_level",
            columnNames = {"budget_id", "period_key", "level"}))
@Getter
@Setter
@NoArgsConstructor
public class BudgetAlertEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "alert_id")
  private Long alertId;

  @Column(name = "budget_id", nullable = false)
  private Long budgetId;

  @Column(name = "user_id", nullable = false, length = 64)
  private String userId;

  /** 예산 기간 식별자 (예: 2026-08). */
  @Column(name = "period_key", nullable = false, length = 20)
  private String periodKey;

  @Enumerated(EnumType.STRING)
  @Column(name = "level", nullable = false, length = 20)
  private Level level;

  @Column(name = "spent_amount", nullable = false)
  private Long spentAmount;

  @Column(name = "budget_amount", nullable = false)
  private Long budgetAmount;

  @Column(name = "notified_at", nullable = false)
  private LocalDateTime notifiedAt;

  public enum Level {
    /** 임계치 도달 (기본 80%). */
    THRESHOLD,
    /** 예산 초과. */
    EXCEEDED
  }
}
