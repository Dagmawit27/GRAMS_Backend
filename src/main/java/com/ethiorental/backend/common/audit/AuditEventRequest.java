package com.ethiorental.backend.common.audit;
import java.time.Instant;
public record AuditEventRequest( 
    AuditAction action,
    String module,
    String entityId,
    String previousStatus,
    String newStatus,
    AuditOutcome outcome,
    String reason,
    String correlationId
     // actor, role, session, IP, user-agent are pulled from SecurityContext/request in the aspect,
    // not passed in manually — keeps callers simple
){}
