/**
 *
 *
 * <pre>
 * <b>Description  : 현금흐름 엔티티 (CashflowForecastEntity)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.domain.cashflow.entity
 * </pre>
 *
 * @author : RosieOh
 * @version : 1.0
 * @since
 *     <pre>
 * Modification Information
 *    수정일              수정자                수정내용
 * ---------------   ---------------   ----------------------------
 *  2026.06.15        RosieOh     최초생성
 *        </pre>
 */
package com.burty.domain.cashflow.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "tbl_cashflow_forecast",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_forecast_user_date",
          columnNames = {"user_id", "forecast_date"})
    },
    indexes = {@Index(name = "idx_forecast_user_date", columnList = "user_id, forecast_date")})
@Getter
@Setter
@NoArgsConstructor
public class CashflowForecastEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "forecast_id")
  private Long forecastId;

  @Column(name = "user_id", length = 64, nullable = false)
  private String userId;

  @Column(name = "forecast_date", nullable = false)
  private LocalDate forecastDate;

  @Column(name = "opening_balance", nullable = false)
  private Long openingBalance;

  @Column(name = "minimum_balance", nullable = false)
  private Long minimumBalance;

  @Column(name = "risk_date")
  private LocalDate riskDate;

  @Column(name = "risk_reason", length = 500)
  private String riskReason;

  @Column(name = "horizon_days", nullable = false)
  private Integer horizonDays = 30;

  @Column(name = "actual_min_balance")
  private Long actualMinBalance;

  @Column(name = "accuracy_pct")
  private Double accuracyPct;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @PrePersist
  void onCreate() {
    if (createdAt == null) createdAt = LocalDateTime.now();
  }
}
