/**
 *
 *
 * <pre>
 * <b>Description  : 행동추천 엔티티 (ActionExecutionEntity)</b>
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
@Table(name = "tbl_action_execution")
@Getter
@Setter
@NoArgsConstructor
public class ActionExecutionEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "execution_id")
  private Long executionId;

  @Column(name = "user_id", nullable = false, length = 64)
  private String userId;

  @Column(name = "action_type", nullable = false, length = 64)
  private String actionType;

  @Column(name = "executed", nullable = false)
  private boolean executed;

  @Column(name = "message")
  private String message;

  @Column(name = "executed_at", nullable = false)
  private LocalDateTime executedAt;
}
