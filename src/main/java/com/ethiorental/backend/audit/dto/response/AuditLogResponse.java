package com.ethiorental.backend.audit.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogResponse {
    private UUID id;
    private String eventType;
    private Instant timestamp;
    private String actorId;
    private String actorRole;
    private String sessionId;
    private String module;
    private String entityType;
    private String entityId;
    private String previousStatus;
    private String newStatus;
    private String outcome;
    private String ipAddress;
    private String userAgent;
    private String correlationId;
    private String reason;
}
