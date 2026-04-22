package com.nuri.domain.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

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

    @Column(name = "actor_id", columnDefinition = "BINARY(16)")
    private UUID actorId;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    public enum EventType { CREATED, AUTH_REQUESTED, AUTH_APPROVED, AUTH_REJECTED, BANK_CALLED, BANK_RESPONDED, EXECUTED, FAILED, CANCELLED, RETRIED }
    public enum ActorType { USER, SYSTEM, AI_AGENT, BANK, SCHEDULER }
}
