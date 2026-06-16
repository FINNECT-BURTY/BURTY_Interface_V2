/**
 *
 *
 * <pre>
 * <b>Description  : 거래 엔티티 (CategoryRuleEntity)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.domain.transaction.entity
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
package com.burty.domain.transaction.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "tbl_category_rule",
    indexes = {@Index(name = "idx_rule_active_priority", columnList = "active, priority")})
@Getter
@Setter
@NoArgsConstructor
public class CategoryRuleEntity {

  @Id
  @Column(name = "rule_id", length = 64)
  private String ruleId;

  @Column(name = "merchant_pattern", length = 120, nullable = false)
  private String merchantPattern;

  @Column(name = "match_type", length = 16, nullable = false)
  private String matchType = "CONTAINS";

  @Column(name = "expense_category_code", length = 40)
  private String expenseCategoryCode;

  @Column(name = "income_category_code", length = 40)
  private String incomeCategoryCode;

  @Column(name = "priority", nullable = false)
  private Integer priority = 50;

  @Column(name = "source", length = 16, nullable = false)
  private String source = "SYSTEM";

  @Column(name = "active", nullable = false)
  private Boolean active = true;

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
