package com.burty.adapter.out.audit;

import com.burty.application.port.out.AuditLogPort;
import com.burty.domain.entity.AuditLogEntity;
import com.burty.domain.model.AuditEvent;
import com.burty.domain.repository.AuditLogRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Primary
@Component
public class JpaAuditLogAdapter implements AuditLogPort {
    private final AuditLogRepository auditLogRepository;

    public JpaAuditLogAdapter(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    public void save(AuditEvent event) {
        AuditLogEntity entity = new AuditLogEntity();
        entity.setOccurredAt(event.getCreatedAt());
        entity.setActorType(AuditLogEntity.ActorType.USER);
        entity.setActorId(parseUuidOrNull(event.getActorId()));
        entity.setTargetType(event.getTarget() == null ? "MONEYAGENT" : event.getTarget());
        entity.setAction(event.getAction());
        entity.setResult(toResult(event.getResult()));
        entity.setRequestId(event.getTraceId());
        entity.setMetadata("{\"detail\":\"" + escape(event.getDetail()) + "\"}");
        auditLogRepository.save(entity);
    }

    private AuditLogEntity.Result toResult(String result) {
        if ("FAILED".equalsIgnoreCase(result)) return AuditLogEntity.Result.FAILED;
        if ("DENIED".equalsIgnoreCase(result)) return AuditLogEntity.Result.DENIED;
        return AuditLogEntity.Result.SUCCESS;
    }

    private UUID parseUuidOrNull(String value) {
        try {
            return value == null ? null : UUID.fromString(value);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String escape(String value) {
        return value == null ? "" : value.replace("\"", "'");
    }
}
