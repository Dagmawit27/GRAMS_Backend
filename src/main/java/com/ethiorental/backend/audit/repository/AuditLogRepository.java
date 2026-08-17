package com.ethiorental.backend.audit.repository;

import com.ethiorental.backend.audit.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID>, JpaSpecificationExecutor<AuditLog> {

    @Query("""
        SELECT a FROM AuditLog a
        WHERE (:module IS NULL OR a.module = :module)
          AND (:actorId IS NULL OR a.actorId = :actorId)
          AND (:action IS NULL OR a.eventType = :action)
          AND (:outcome IS NULL OR a.outcome = :outcome)
          AND (:startDate IS NULL OR a.timestamp >= :startDate)
          AND (:endDate IS NULL OR a.timestamp <= :endDate)
    """)
    Page<AuditLog> findFilteredLogs(
            @Param("module") String module,
            @Param("actorId") String actorId,
            @Param("action") String action,
            @Param("outcome") String outcome,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            Pageable pageable
    );

    @Query("""
        SELECT a FROM AuditLog a
        WHERE a.eventType IN ('LOGIN', 'LOGOUT', 'LOGIN_FAILED', 'LOCKOUT', 'ROLE_CHANGE', 'CONFIG_CHANGE')
          AND (:startDate IS NULL OR a.timestamp >= :startDate)
          AND (:endDate IS NULL OR a.timestamp <= :endDate)
    """)
    Page<AuditLog> findSecurityEvents(
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            Pageable pageable
    );

    // Override delete methods to throw UnsupportedOperationException to guarantee append-only
    @Override
    default void deleteById(UUID id) {
        throw new UnsupportedOperationException("Audit logs are append-only. Delete operations are strictly forbidden.");
    }

    @Override
    default void delete(AuditLog entity) {
        throw new UnsupportedOperationException("Audit logs are append-only. Delete operations are strictly forbidden.");
    }

    @Override
    default void deleteAll(Iterable<? extends AuditLog> entities) {
        throw new UnsupportedOperationException("Audit logs are append-only. Delete operations are strictly forbidden.");
    }

    @Override
    default void deleteAll() {
        throw new UnsupportedOperationException("Audit logs are append-only. Delete operations are strictly forbidden.");
    }
}
