package com.berty.application.port.out;

import com.berty.domain.model.AuditEvent;

public interface AuditLogPort {
    void save(AuditEvent event);
}
