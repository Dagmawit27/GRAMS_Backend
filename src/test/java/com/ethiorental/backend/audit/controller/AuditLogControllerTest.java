package com.ethiorental.backend.audit.controller;

import com.ethiorental.backend.audit.entity.AuditLog;
import com.ethiorental.backend.audit.repository.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditLogControllerTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditLogController controller;

    @Test
    void getAuditLogs_ShouldReturnPaginatedAuditLogs() {
        AuditLog logEntry = AuditLog.builder()
                .id(UUID.randomUUID())
                .eventType("APPROVE")
                .module("PROPERTY")
                .entityId("PROP-100")
                .actorId("admin@grams.gov.et")
                .outcome("SUCCESS")
                .timestamp(Instant.now())
                .build();

        Page<AuditLog> page = new PageImpl<>(List.of(logEntry));
        when(auditLogRepository.findFilteredLogs(any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        ResponseEntity<Page<com.ethiorental.backend.audit.dto.response.AuditLogResponse>> response =
                controller.getAuditLogs("PROPERTY", "admin@grams.gov.et", "APPROVE", "SUCCESS", null, null, 0, 20);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getTotalElements());
        assertEquals("APPROVE", response.getBody().getContent().get(0).getEventType());
    }

    @Test
    void getSecurityEvents_ShouldReturnPaginatedSecurityLogs() {
        AuditLog securityLog = AuditLog.builder()
                .id(UUID.randomUUID())
                .eventType("LOGIN")
                .module("IAM")
                .actorId("officer@grams.gov.et")
                .outcome("SUCCESS")
                .timestamp(Instant.now())
                .build();

        Page<AuditLog> page = new PageImpl<>(List.of(securityLog));
        when(auditLogRepository.findSecurityEvents(any(), any(), any(Pageable.class)))
                .thenReturn(page);

        ResponseEntity<Page<com.ethiorental.backend.audit.dto.response.AuditLogResponse>> response =
                controller.getSecurityEvents(null, null, 0, 20);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getTotalElements());
        assertEquals("LOGIN", response.getBody().getContent().get(0).getEventType());
    }
}
