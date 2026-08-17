package com.ethiorental.backend.audit.service;

import com.ethiorental.backend.shared.audit.AuditAction;
import com.ethiorental.backend.shared.audit.AuditEventRequest;
import com.ethiorental.backend.shared.audit.AuditOutcome;
import com.ethiorental.backend.shared.audit.AuditService;
import com.ethiorental.backend.shared.audit.Auditable;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditAspectTest {

    @Mock
    private AuditService auditService;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature methodSignature;

    @Mock
    private Auditable auditable;

    @InjectMocks
    private AuditAspect aspect;

    @Test
    void around_ShouldLogSuccess_WhenMethodExecutesSuccessfully() throws Throwable {
        when(auditable.action()).thenReturn(AuditAction.APPROVE);
        when(auditable.module()).thenReturn("PROPERTY");
        when(auditable.entityIdParam()).thenReturn("propertyId");

        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getParameterNames()).thenReturn(new String[]{"propertyId", "officerNotes"});
        when(joinPoint.getArgs()).thenReturn(new Object[]{"PROP-999", "Approved after inspection"});
        when(joinPoint.proceed()).thenReturn("SUCCESS_RESULT");

        Object result = aspect.around(joinPoint, auditable);

        assertEquals("SUCCESS_RESULT", result);

        ArgumentCaptor<AuditEventRequest> captor = ArgumentCaptor.forClass(AuditEventRequest.class);
        verify(auditService).log(captor.capture());

        AuditEventRequest event = captor.getValue();
        assertEquals(AuditAction.APPROVE, event.action());
        assertEquals("PROPERTY", event.module());
        assertEquals("PROP-999", event.entityId());
        assertEquals(AuditOutcome.SUCCESS, event.outcome());
    }

    @Test
    void around_ShouldLogFailure_WhenMethodThrowsException() throws Throwable {
        when(auditable.action()).thenReturn(AuditAction.DELETE);
        when(auditable.module()).thenReturn("AGREEMENT");
        when(auditable.entityIdParam()).thenReturn("agreementId");

        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getParameterNames()).thenReturn(new String[]{"agreementId"});
        when(joinPoint.getArgs()).thenReturn(new Object[]{"AGR-100"});
        when(joinPoint.proceed()).thenThrow(new IllegalArgumentException("Agreement cannot be deleted"));

        assertThrows(IllegalArgumentException.class, () -> aspect.around(joinPoint, auditable));

        ArgumentCaptor<AuditEventRequest> captor = ArgumentCaptor.forClass(AuditEventRequest.class);
        verify(auditService).log(captor.capture());

        AuditEventRequest event = captor.getValue();
        assertEquals(AuditAction.DELETE, event.action());
        assertEquals("AGREEMENT", event.module());
        assertEquals("AGR-100", event.entityId());
        assertEquals(AuditOutcome.FAILURE, event.outcome());
        assertEquals("Agreement cannot be deleted", event.reason());
    }
}
