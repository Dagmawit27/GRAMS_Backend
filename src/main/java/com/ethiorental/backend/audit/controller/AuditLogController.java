package com.ethiorental.backend.audit.controller;

import com.ethiorental.backend.audit.dto.response.AuditLogResponse;
import com.ethiorental.backend.audit.entity.AuditLog;
import com.ethiorental.backend.audit.repository.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/audit")
@PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'AUDITOR')")
public class AuditLogController {

    private final AuditLogRepository auditLogRepository;

    public AuditLogController(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * GET /api/v1/audit/logs
     * Paginated and filterable audit log retrieval for ADMIN and AUDITOR roles.
     */
    @GetMapping("/logs")
    public ResponseEntity<Page<AuditLogResponse>> getAuditLogs(
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String actorId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String outcome,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        Page<AuditLog> logs = auditLogRepository.findFilteredLogs(module, actorId, action, outcome, startDate, endDate, pageable);
        Page<AuditLogResponse> response = logs.map(this::mapToResponse);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/audit/security-events
     * Paginated security events retrieval (login, logout, failed logins, role changes, lockouts).
     */
    @GetMapping("/security-events")
    public ResponseEntity<Page<AuditLogResponse>> getSecurityEvents(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        Page<AuditLog> securityLogs = auditLogRepository.findSecurityEvents(startDate, endDate, pageable);
        Page<AuditLogResponse> response = securityLogs.map(this::mapToResponse);
        return ResponseEntity.ok(response);
    }

    private AuditLogResponse mapToResponse(AuditLog log) {
        return AuditLogResponse.builder()
                .id(log.getId())
                .eventType(log.getEventType())
                .timestamp(log.getTimestamp())
                .actorId(log.getActorId())
                .actorRole(log.getActorRole())
                .sessionId(log.getSessionId())
                .module(log.getModule())
                .entityType(log.getEntityType())
                .entityId(log.getEntityId())
                .previousStatus(log.getPreviousStatus())
                .newStatus(log.getNewStatus())
                .outcome(log.getOutcome())
                .ipAddress(log.getIpAddress())
                .userAgent(log.getUserAgent())
                .correlationId(log.getCorrelationId())
                .reason(log.getReason())
                .build();
    }
}