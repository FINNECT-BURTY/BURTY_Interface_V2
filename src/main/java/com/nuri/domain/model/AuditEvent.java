package com.nuri.domain.model;

import java.time.LocalDateTime;

public class AuditEvent {
    private String traceId;
    private String actorId;
    private String action;
    private String target;
    private String result;
    private String detail;
    private LocalDateTime createdAt;

    public AuditEvent(String traceId, String actorId, String action, String target, String result, String detail, LocalDateTime createdAt) {
        this.traceId = traceId;
        this.actorId = actorId;
        this.action = action;
        this.target = target;
        this.result = result;
        this.detail = detail;
        this.createdAt = createdAt;
    }

    public String getTraceId() { return traceId; }
    public String getActorId() { return actorId; }
    public String getAction() { return action; }
    public String getTarget() { return target; }
    public String getResult() { return result; }
    public String getDetail() { return detail; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
