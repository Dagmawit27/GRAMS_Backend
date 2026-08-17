package com.ethiorental.backend.audit.service;

import com.ethiorental.backend.audit.entity.AuditLog;
import com.ethiorental.backend.audit.repository.AuditLogRepository;
import com.ethiorental.backend.shared.audit.AuditAction;
import com.ethiorental.backend.shared.audit.AuditEventRequest;
import com.ethiorental.backend.shared.audit.AuditOutcome;
import com.ethiorental.backend.shared.audit.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AuditServiceImpl implements AuditService{

    private final AuditLogRepository repository;

    private static final Pattern FAYDA_ID_PATTERN = Pattern.compile("\\b\\d{12}\\b");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("(?i)(password|pass|secret|otp|token)=[^&\\s]+");
    private static final Pattern BEARER_PATTERN = Pattern.compile("(?i)Bearer\\s+[A-Za-z0-9._-]+");

    public AuditServiceImpl(AuditLogRepository repository) {
        this.repository = repository;
    }

    @Override
    public void log(AuditEventRequest event) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String actorId = "ANONYMOUS";
            String actorRole = "UNAUTHENTICATED";
            String sessionId = null;

            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                actorId = auth.getName();
                actorRole = auth.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.joining(","));
            }

            String ipAddress = null;
            String userAgent = null;

            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest httpRequest = attributes.getRequest();
                ipAddress = extractIpAddress(httpRequest);
                userAgent = httpRequest.getHeader("User-Agent");
                if (httpRequest.getSession(false) != null) {
                    sessionId = httpRequest.getSession(false).getId();
                }
            }

            String redactedReason = redactSensitiveData(event.reason());
            String redactedEntityId = redactSensitiveData(event.entityId());

            AuditLog entry = AuditLog.builder()
                    .eventType(event.action() != null ? event.action().name() : "UNKNOWN")
                    .timestamp(Instant.now())
                    .actorId(actorId)
                    .actorRole(actorRole)
                    .sessionId(sessionId)
                    .module(event.module() != null ? event.module() : "GENERAL")
                    .entityType(event.module())
                    .entityId(redactedEntityId)
                    .previousStatus(event.previousStatus())
                    .newStatus(event.newStatus())
                    .outcome(event.outcome() != null ? event.outcome().name() : AuditOutcome.SUCCESS.name())
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .correlationId(event.correlationId())
                    .reason(redactedReason)
                    .build();

            // Fail-open-but-alert behavior: wrap persistence in try-catch
            repository.save(entry);
        } catch (Exception ex) {
            // Audit failures must never silently swallow, but must not crash the business operation
            log.error("AUDIT_WRITE_ALERT: Failed to persist audit log event for action [{}], module [{}]. Error: {}",
                    event != null ? event.action() : "UNKNOWN",
                    event != null ? event.module() : "UNKNOWN",
                    ex.getMessage(),
                    ex);
        }
    }

    @Override
    public void logStatusChange(String module, String entityId, String previousStatus, String newStatus, AuditAction action, String reason) {
        log(new AuditEventRequest(
                action,
                module,
                entityId,
                previousStatus,
                newStatus,
                AuditOutcome.SUCCESS,
                reason,
                null
        ));
    }

    private String extractIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    public String redactSensitiveData(String input) {
        if (input == null || input.isBlank()) {
            return input;
        }
        String redacted = FAYDA_ID_PATTERN.matcher(input).replaceAll("[REDACTED_FAYDA_ID]");
        redacted = PASSWORD_PATTERN.matcher(redacted).replaceAll("$1=[REDACTED]");
        redacted = BEARER_PATTERN.matcher(redacted).replaceAll("Bearer [REDACTED_TOKEN]");
        return redacted;
    }
}
