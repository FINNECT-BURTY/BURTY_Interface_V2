/**
 *
 *
 * <pre>
 * <b>Description  : 공통지원 (AuditLogger)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.application.service.support
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
package com.burty.application.service.support;

import com.burty.application.port.out.audit.AuditLogPort;
import com.burty.domain.admin.model.AuditEvent;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuditLogger {

  private static final Logger log = LoggerFactory.getLogger(AuditLogger.class);

  private final AuditLogPort auditLogPort;

  public void logSuccess(String actorId, String action, String target, String detail) {
    log(actorId, action, target, "SUCCESS", detail);
  }

  public void log(String actorId, String action, String target, String result, String detail) {
    try {
      auditLogPort.save(
          new AuditEvent(
              UUID.randomUUID().toString(),
              actorId,
              action,
              target,
              result,
              detail,
              LocalDateTime.now()));
    } catch (Exception e) {
      log.warn(
          "audit save failed actorId={} action={} reason={}",
          actorId,
          action,
          e.getClass().getSimpleName());
    }
  }
}
