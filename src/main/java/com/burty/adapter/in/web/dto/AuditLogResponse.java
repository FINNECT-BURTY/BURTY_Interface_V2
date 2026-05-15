package com.burty.adapter.in.web.dto;

import java.time.LocalDateTime;

public class AuditLogResponse {
    private Long auditId;
    private LocalDateTime occurredAt;
    private String actorType;
    private String action;
    private String result;
    private String targetType;
    private String metadata;

    public AuditLogResponse() {}

    public AuditLogResponse(Long auditId, LocalDateTime occurredAt, String actorType, String action,
                            String result, String targetType, String metadata) {
        this.auditId = auditId;
        this.occurredAt = occurredAt;
        this.actorType = actorType;
        this.action = action;
        this.result = result;
        this.targetType = targetType;
        this.metadata = metadata;
    }

    public Long getAuditId() { return auditId; }
    public void setAuditId(Long auditId) { this.auditId = auditId; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
    public void setOccurredAt(LocalDateTime occurredAt) { this.occurredAt = occurredAt; }
    public String getActorType() { return actorType; }
    public void setActorType(String actorType) { this.actorType = actorType; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }
    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
}
