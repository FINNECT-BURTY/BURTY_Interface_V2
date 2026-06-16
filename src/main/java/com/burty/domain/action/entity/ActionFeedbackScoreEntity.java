/**
 *
 *
 * <pre>
 * <b>Description  : 행동추천 엔티티 (ActionFeedbackScoreEntity)</b>
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
    name = "tbl_action_feedback_score",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_feedback_score_user_action",
          columnNames = {"user_id", "action_type_code"})
    })
@Getter
@Setter
@NoArgsConstructor
public class ActionFeedbackScoreEntity {

  @Id
  @Column(name = "score_id", length = 100)
  private String scoreId;

  @Column(name = "user_id", length = 64, nullable = false)
  private String userId;

  @Column(name = "action_type_code", length = 40, nullable = false)
  private String actionTypeCode;

  @Column(name = "accept_count", nullable = false)
  private Integer acceptCount = 0;

  @Column(name = "reject_count", nullable = false)
  private Integer rejectCount = 0;

  @Column(name = "score", nullable = false)
  private Integer score = 0;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @PrePersist
  @PreUpdate
  void onTouch() {
    if (scoreId == null) scoreId = userId + "|" + actionTypeCode;
    updatedAt = LocalDateTime.now();
  }
}
