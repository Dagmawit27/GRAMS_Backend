package com.ethiorental.backend.audit.service;

import com.ethiorental.backend.shared.audit.AuditEventRequest;
import com.ethiorental.backend.shared.audit.AuditOutcome;
import com.ethiorental.backend.shared.audit.AuditService;
import com.ethiorental.backend.shared.audit.Auditable;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class AuditAspect {

    private final AuditService auditService;

    public AuditAspect(AuditService auditService) {
        this.auditService = auditService;
    }

    @Around("@annotation(auditable)")
    public Object around(ProceedingJoinPoint pjp, Auditable auditable) throws Throwable {
        AuditOutcome outcome = AuditOutcome.SUCCESS;
        String reason = null;
        try {
            Object result = pjp.proceed();
            return result;
        } catch (Throwable ex) {
            outcome = AuditOutcome.FAILURE;
            reason = ex.getMessage();
            throw ex;
        } finally {
            String entityId = extractEntityId(pjp, auditable.entityIdParam());
            auditService.log(new AuditEventRequest(
                    auditable.action(),
                    auditable.module(),
                    entityId,
                    null,
                    null,
                    outcome,
                    reason,
                    null
            ));
        }
    }

    private String extractEntityId(ProceedingJoinPoint pjp, String paramName) {
        if (paramName == null || paramName.isBlank()) return null;
        MethodSignature sig = (MethodSignature) pjp.getSignature();
        String[] names = sig.getParameterNames();
        Object[] args = pjp.getArgs();
        if (names != null && args != null) {
            for (int i = 0; i < names.length; i++) {
                if (names[i].equals(paramName) && args[i] != null) {
                    return String.valueOf(args[i]);
                }
            }
        }
        return null;
    }
}