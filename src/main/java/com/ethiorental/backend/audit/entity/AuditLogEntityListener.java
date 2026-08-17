package com.ethiorental.backend.audit.entity;

import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;

public class AuditLogEntityListener {

    @PreUpdate
    public void preUpdate(AuditLog entity) {
        throw new UnsupportedOperationException("Audit logs are immutable and append-only. Modification is strictly prohibited.");
    }

    @PreRemove
    public void preRemove(AuditLog entity) {
        throw new UnsupportedOperationException("Audit logs are immutable and append-only. Deletion is strictly prohibited.");
    }
}
