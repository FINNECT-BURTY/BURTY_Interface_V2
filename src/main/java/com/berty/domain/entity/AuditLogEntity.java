package com.berty.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tbl_audit_log")
@Getter
@Setter
@NoArgsConstructor
public class AuditLogEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_id")
    private Long auditId;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_type", nullable = false)
    private ActorType actorType;

    @Column(name = "actor_id", columnDefinition = "BINARY(16)")
    private UUID actorId;

    @Column(name = "target_type", nullable = false)
    private String targetType;

    @Column(name = "target_id", columnDefinition = "BINARY(16)")
    private UUID targetId;

    @Column(name = "action", nullable = false)
    private String action;

    @Enumerated(EnumType.STRING)
    @Column(name = "result", nullable = false)
    private Result result;

    @Column(name = "ip_address")
    private byte[] ipAddress;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "request_id", length = 36)
    private String requestId;

    @Column(name = "session_id", length = 36)
    private String sessionId;

    @Column(name = "before_snapshot", columnDefinition = "JSON")
    private String beforeSnapshot;

    @Column(name = "after_snapshot", columnDefinition = "JSON")
    private String afterSnapshot;

    @Column(name = "metadata", columnDefinition = "JSON")
    private String metadata;

    @Column(name = "error_code")
    private String errorCode;

    @Column(name = "error_message")
    private String errorMessage;

    public enum ActorType { USER, GUARDIAN, SYSTEM, AI_AGENT, BANK, ADMIN, SCHEDULER }
    public enum Result { SUCCESS, FAILED, DENIED }
}
