package com.burty.application.port.out;

import com.burty.domain.model.AuditEvent;

public interface AuditLogPort {
    void save(AuditEvent event);
}
