package com.nuri.application.port.out;

import com.nuri.domain.model.AuditEvent;

public interface AuditLogPort {
    void save(AuditEvent event);
}
