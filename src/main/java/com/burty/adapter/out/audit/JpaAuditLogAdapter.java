/**
 *
 *
 * <pre>
 * <b>Description  : 외부연동 외부 연동 어댑터 (JpaAuditLogAdapter)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.adapter.out.audit
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
package com.burty.adapter.out.audit;

import com.burty.application.port.out.audit.AuditLogPort;
import com.burty.domain.admin.entity.AuditLogEntity;
import com.burty.domain.admin.model.AuditEvent;
import com.burty.domain.admin.repository.AuditLogRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

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
    entity.setOccurredAt(event.createdAt());
    entity.setActorType(AuditLogEntity.ActorType.USER);
    entity.setActorId(parseLongOrNull(event.actorId()));
    entity.setTargetType(event.target() == null ? "BURTY" : event.target());
    entity.setAction(event.action());
    entity.setResult(toResult(event.result()));
    entity.setRequestId(event.traceId());
    entity.setMetadata("{\"detail\":\"" + escape(event.detail()) + "\"}");
    auditLogRepository.save(entity);
  }

  private AuditLogEntity.Result toResult(String result) {
    if ("FAILED".equalsIgnoreCase(result)) return AuditLogEntity.Result.FAILED;
    if ("DENIED".equalsIgnoreCase(result)) return AuditLogEntity.Result.DENIED;
    return AuditLogEntity.Result.SUCCESS;
  }

  private Long parseLongOrNull(String value) {
    try {
      return value == null ? null : Long.parseLong(value);
    } catch (Exception ignored) {
      return null;
    }
  }

  private String escape(String value) {
    return value == null ? "" : value.replace("\"", "'");
  }
}
