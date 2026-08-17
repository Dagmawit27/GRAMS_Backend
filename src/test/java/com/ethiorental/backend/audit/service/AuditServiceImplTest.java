package com.ethiorental.backend.audit.service;

import com.ethiorental.backend.audit.entity.AuditLog;
import com.ethiorental.backend.audit.repository.AuditLogRepository;
import com.ethiorental.backend.shared.audit.AuditAction;
import com.ethiorental.backend.shared.audit.AuditEventRequest;
import com.ethiorental.backend.shared.audit.AuditOutcome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditServiceImplTest {

    @Mock
    private AuditLogRepository repository;

    @InjectMocks
    private AuditServiceImpl auditService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void log_ShouldPopulateFieldsAndSaveAuditEntry() {
        // Arrange: mock SecurityContext
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "officer@grams.gov.et",
                "credentials",
                List.of(new SimpleGrantedAuthority("ROLE_WOREDA_OFFICER"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        AuditEventRequest request = new AuditEventRequest(
                AuditAction.APPROVE,
                "PROPERTY",
                "PROP-1001",
                "PENDING",
                "APPROVED",
                AuditOutcome.SUCCESS,
                "Property registration verified",
                "CORR-123"
        );

        // Act
        auditService.log(request);

        // Assert: verify repository.save was called with populated AuditLog
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(repository, times(1)).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertEquals("APPROVE", saved.getEventType());
        assertEquals("PROPERTY", saved.getModule());
        assertEquals("PROP-1001", saved.getEntityId());
        assertEquals("officer@grams.gov.et", saved.getActorId());
        assertEquals("ROLE_WOREDA_OFFICER", saved.getActorRole());
        assertEquals("SUCCESS", saved.getOutcome());
        assertEquals("Property registration verified", saved.getReason());
    }

    @Test
    void log_ShouldRedactSensitiveValuesInReasonAndEntityId() {
        AuditEventRequest request = new AuditEventRequest(
                AuditAction.REGISTER,
                "IAM",
                "123456789012", // 12-digit Fayda ID
                null,
                "ACTIVE",
                AuditOutcome.SUCCESS,
                "Citizen user registered password=Secret123! token=Bearer eyJhbGciOiJIUzI1NiJ9",
                "CORR-999"
        );

        auditService.log(request);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(repository).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertEquals("[REDACTED_FAYDA_ID]", saved.getEntityId());
        assertTrue(saved.getReason().contains("password=[REDACTED]"));
        assertTrue(saved.getReason().contains("Bearer [REDACTED_TOKEN]"));
        assertFalse(saved.getReason().contains("Secret123!"));
    }

    @Test
    void log_ShouldFailOpenWhenRepositoryThrowsException() {
        // Arrange: mock repository throwing DB exception
        doThrow(new RuntimeException("Database connection timeout")).when(repository).save(any());

        AuditEventRequest request = new AuditEventRequest(
                AuditAction.UPDATE,
                "AGREEMENT",
                "AGR-500",
                "DRAFT",
                "ACTIVE",
                AuditOutcome.SUCCESS,
                "Agreement activated",
                null
        );

        // Act & Assert: method should NOT throw exception to caller (fail-open)
        assertDoesNotThrow(() -> auditService.log(request));
    }
}
