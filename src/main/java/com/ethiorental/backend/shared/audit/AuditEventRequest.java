package com.ethiorental.backend.shared.audit;

public record AuditEventRequest(
    AuditAction action,
    String module,
    String entityId,
    String previousStatus,
    String newStatus,
    AuditOutcome outcome,
    String reason,
    String correlationId
) {}
