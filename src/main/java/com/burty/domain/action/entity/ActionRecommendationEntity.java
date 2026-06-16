/**
 *
 *
 * <pre>
 * <b>Description  : 행동추천 엔티티 (ActionRecommendationEntity)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.domain.action.entity
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
package com.burty.domain.action.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "tbl_action_recommendation",
    indexes = {
      @Index(name = "idx_action_active_score", columnList = "active, base_score"),
      @Index(name = "idx_action_persona", columnList = "occupation_code")
    })
@Getter
@Setter
@NoArgsConstructor
public class ActionRecommendationEntity {

  @Id
  @Column(name = "rec_id", length = 80)
  private String recId;

  @Column(name = "action_type_code", length = 40, nullable = false)
  private String actionTypeCode;

  @Column(name = "title_template", length = 200, nullable = false)
  private String titleTemplate;

  @Column(name = "description_template", length = 500, nullable = false)
  private String descriptionTemplate;

  @Column(name = "base_score", nullable = false)
  private Double baseScore = 50.0;

  @Column(name = "estimated_improvement", nullable = false)
  private Long estimatedImprovement = 0L;

  @Column(name = "occupation_code", length = 40)
  private String occupationCode;

  @Column(name = "min_min_balance")
  private Long minMinBalance;

  @Column(name = "max_min_balance")
  private Long maxMinBalance;

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
