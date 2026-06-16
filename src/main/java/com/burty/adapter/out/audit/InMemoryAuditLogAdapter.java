/**
 *
 *
 * <pre>
 * <b>Description  : 외부연동 외부 연동 어댑터 (InMemoryAuditLogAdapter)</b>
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
import com.burty.core.constant.LogMessages;
import com.burty.domain.admin.model.AuditEvent;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnMissingBean(AuditLogPort.class)
public class InMemoryAuditLogAdapter implements AuditLogPort {
  private final CopyOnWriteArrayList<AuditEvent> store = new CopyOnWriteArrayList<>();

  @Override
  public void save(AuditEvent event) {
    store.add(event);
    log.info(
        LogMessages.Audit.TRACE,
        event.traceId(),
        event.actorId(),
        event.action(),
        event.target(),
        event.result());
  }
}
