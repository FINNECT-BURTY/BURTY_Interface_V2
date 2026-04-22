package com.nuri.adapter.out.audit;

import com.nuri.application.port.out.AuditLogPort;
import com.nuri.domain.model.AuditEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Component
@ConditionalOnMissingBean(AuditLogPort.class)
public class InMemoryAuditLogAdapter implements AuditLogPort {
    private final CopyOnWriteArrayList<AuditEvent> store = new CopyOnWriteArrayList<>();

    @Override
    public void save(AuditEvent event) {
        store.add(event);
        log.info("AUDIT traceId={} actor={} action={} target={} result={}",
                event.getTraceId(), event.getActorId(), event.getAction(), event.getTarget(), event.getResult());
    }
}
