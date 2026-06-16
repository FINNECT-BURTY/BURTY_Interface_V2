/**
 *
 *
 * <pre>
 * <b>Description  : 사용자 엔티티 (PersonaProfileEntity)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.domain.user.entity
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
package com.burty.domain.user.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "tbl_persona_profile",
    indexes = {@Index(name = "idx_persona_user", columnList = "user_id", unique = true)})
@Getter
@Setter
@NoArgsConstructor
public class PersonaProfileEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "persona_id")
  private Long personaId;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Column(name = "occupation_code", length = 40)
  private String occupationCode;

  @Column(name = "residence_code", length = 40)
  private String residenceCode;

  @Column(name = "household_type", length = 30)
  private String householdType;

  @Column(name = "monthly_income_avg")
  private Long monthlyIncomeAvg;

  @Column(name = "income_variability_pct")
  private Double incomeVariabilityPct;

  @Column(name = "age")
  private Integer age;

  @Column(name = "source", length = 16, nullable = false)
  private String source = "INFERRED";

  @Column(name = "inferred_at")
  private LocalDateTime inferredAt;

  @Column(name = "user_overridden", nullable = false)
  private Boolean userOverridden = false;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @PrePersist
  void onCreate() {
    LocalDateTime now = LocalDateTime.now();
    if (createdAt == null) createdAt = now;
    if (updatedAt == null) updatedAt = now;
  }

  @PreUpdate
  void onUpdate() {
    updatedAt = LocalDateTime.now();
  }
}
