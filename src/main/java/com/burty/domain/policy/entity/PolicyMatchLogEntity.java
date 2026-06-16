/**
 *
 *
 * <pre>
 * <b>Description  : 정책 엔티티 (PolicyMatchLogEntity)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.domain.policy.entity
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
package com.burty.domain.policy.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "tbl_policy_match_log",
    indexes = {
      @Index(name = "idx_policy_match_user", columnList = "user_id, matched_at"),
      @Index(name = "idx_policy_match_policy", columnList = "policy_code")
    })
@Getter
@Setter
@NoArgsConstructor
public class PolicyMatchLogEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "match_log_id")
  private Long matchLogId;

  @Column(name = "user_id", length = 64, nullable = false)
  private String userId;

  @Column(name = "policy_code", length = 64, nullable = false)
  private String policyCode;

  @Column(name = "policy_title", length = 200)
  private String policyTitle;

  @Column(name = "priority_score", nullable = false)
  private Integer priorityScore;

  @Column(name = "rank_in_match", nullable = false)
  private Integer rankInMatch;

  @Column(name = "occupation_code", length = 40)
  private String occupationCode;

  @Column(name = "applied", nullable = false)
  private Boolean applied = false;

  @Column(name = "applied_at")
  private LocalDateTime appliedAt;

  @Column(name = "matched_at", nullable = false)
  private LocalDateTime matchedAt;

  @PrePersist
  void onCreate() {
    if (matchedAt == null) matchedAt = LocalDateTime.now();
  }
}
