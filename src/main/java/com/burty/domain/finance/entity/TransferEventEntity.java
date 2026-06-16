/**
 *
 *
 * <pre>
 * <b>Description  : 금융 엔티티 (TransferEventEntity)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.domain.finance.entity
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
package com.burty.domain.finance.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tbl_transfer_event")
public class TransferEventEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "event_id")
  private Long eventId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "order_id", nullable = false)
  private TransferOrderEntity order;

  @Column(name = "sequence_no", nullable = false)
  private Integer sequenceNo;

  @Enumerated(EnumType.STRING)
  @Column(name = "event_type", nullable = false)
  private EventType eventType;

  @Column(name = "payload", columnDefinition = "JSON")
  private String payload;

  @Enumerated(EnumType.STRING)
  @Column(name = "actor_type", nullable = false)
  private ActorType actorType;

  @Column(name = "actor_id")
  private Long actorId;

  @Column(name = "occurred_at", nullable = false)
  private LocalDateTime occurredAt;

  public enum EventType {
    CREATED,
    AUTH_REQUESTED,
    AUTH_APPROVED,
    AUTH_REJECTED,
    BANK_CALLED,
    BANK_RESPONDED,
    EXECUTED,
    FAILED,
    CANCELLED,
    RETRIED
  }

  public enum ActorType {
    USER,
    SYSTEM,
    AI_AGENT,
    BANK,
    SCHEDULER
  }
}
