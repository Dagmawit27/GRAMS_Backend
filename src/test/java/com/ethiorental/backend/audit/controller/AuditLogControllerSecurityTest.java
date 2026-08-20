package com.ethiorental.backend.audit.controller;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies that the {@code @PreAuthorize} on {@link AuditLogController}
 * uses correct role names without redundant {@code ROLE_} prefixes.
 * <p>
 * This test would have caught the redundant ROLE_ prefix bug
 * ({@code hasAnyRole('ROLE_SYSTEM_ADMINISTRATOR')} checked for
 * {@code ROLE_ROLE_SYSTEM_ADMINISTRATOR} and never matched).
 * <p>
 * Note: Runtime enforcement of @PreAuthorize requires a Spring context with
 * AOP proxying (e.g. @WebMvcTest or @SpringBootTest). This test verifies
 * the annotation metadata statically, which is sufficient to catch the
 * role-name typo. Runtime access-control tests are deferred to integration
 * testing when a full Spring context with security is available.
 */
class AuditLogControllerSecurityTest {

    private static final Pattern ROLE_PATTERN = Pattern.compile("'([A-Z_]+)'");

    @Test
    void preAuthorizeOnControllerUsesCorrectRoleNames() {
        PreAuthorize annotation = AuditLogController.class.getAnnotation(PreAuthorize.class);
        assertNotNull(annotation, "AuditLogController should have @PreAuthorize annotation");

        String SpEL = annotation.value();
        Matcher matcher = ROLE_PATTERN.matcher(SpEL);

        assertTrue(matcher.find(), "@PreAuthorize should contain at least one role name");
        String roleName = matcher.group(1);
        assertFalse(roleName.startsWith("ROLE_"),
                "Role name in @PreAuthorize should NOT have ROLE_ prefix (hasAnyRole auto-adds it): " + roleName);
    }

    @Test
    void preAuthorizeContainsSystemAdministratorRole() {
        PreAuthorize annotation = AuditLogController.class.getAnnotation(PreAuthorize.class);
        assertNotNull(annotation);

        String SpEL = annotation.value();
        assertTrue(SpEL.contains("SYSTEM_ADMINISTRATOR"),
                "@PreAuthorize should allow SYSTEM_ADMINISTRATOR role");
        // Verify it's not prefixed
        assertFalse(SpEL.contains("'ROLE_SYSTEM_ADMINISTRATOR'"),
                "Should not use 'ROLE_SYSTEM_ADMINISTRATOR' (hasAnyRole auto-adds ROLE_ prefix)");
    }

    @Test
    void preAuthorizeContainsAuditorRole() {
        PreAuthorize annotation = AuditLogController.class.getAnnotation(PreAuthorize.class);
        assertNotNull(annotation);

        String SpEL = annotation.value();
        assertTrue(SpEL.contains("AUDITOR"),
                "@PreAuthorize should allow AUDITOR role");
        // Verify it's not prefixed
        assertFalse(SpEL.contains("'ROLE_AUDITOR'"),
                "Should not use 'ROLE_AUDITOR' (hasAnyRole auto-adds ROLE_ prefix)");
    }

    @Test
    void preAuthorizeOnlyAllowsIntendedRoles() {
        PreAuthorize annotation = AuditLogController.class.getAnnotation(PreAuthorize.class);
        assertNotNull(annotation);

        String SpEL = annotation.value();
        Matcher matcher = ROLE_PATTERN.matcher(SpEL);

        java.util.List<String> roles = new java.util.ArrayList<>();
        while (matcher.find()) {
            roles.add(matcher.group(1));
        }

        // Only SYSTEM_ADMINISTRATOR and AUDITOR should be allowed
        assertEquals(2, roles.size(),
                "Expected exactly 2 roles in @PreAuthorize, found: " + roles);
        assertTrue(roles.contains("SYSTEM_ADMINISTRATOR"),
                "Allowed roles should include SYSTEM_ADMINISTRATOR");
        assertTrue(roles.contains("AUDITOR"),
                "Allowed roles should include AUDITOR");
    }
}
