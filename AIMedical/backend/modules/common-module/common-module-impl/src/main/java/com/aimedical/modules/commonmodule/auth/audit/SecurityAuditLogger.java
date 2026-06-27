package com.aimedical.modules.commonmodule.auth.audit;

public interface SecurityAuditLogger {
    void logAudit(SecurityAuditEvent event);
}
