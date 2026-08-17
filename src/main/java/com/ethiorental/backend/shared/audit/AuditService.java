package com.ethiorental.backend.shared.audit;

public interface AuditService {
    /**
     * Log an audit event.
     */
    void log(AuditEventRequest event);

    /**
     * Explicit status transition logging for business operations (e.g. property approval, agreement signing).
     */
    void logStatusChange(String module, String entityId, String previousStatus, String newStatus, AuditAction action, String reason);
}
