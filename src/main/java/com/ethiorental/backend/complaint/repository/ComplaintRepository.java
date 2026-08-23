package com.ethiorental.backend.complaint.repository;

import com.ethiorental.backend.complaint.entity.Complaint;
import com.ethiorental.backend.complaint.enums.ComplaintStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, UUID> {

    /** All complaints filed by a specific citizen. */
    List<Complaint> findByComplainant_IdOrderByCreatedAtDesc(UUID citizenId);

    /** Complaints filtered by status — for officer queue. */
    List<Complaint> findByStatusOrderByCreatedAtAsc(ComplaintStatus status);

    // ── Reporting queries ─────────────────────────────────────────────────────
    long countByStatus(ComplaintStatus status);

    /** Returns [status, count] pairs. */
    @Query("SELECT c.status, COUNT(c) FROM Complaint c GROUP BY c.status")
    List<Object[]> countGroupedByStatus();

    /** Returns [category, count] pairs. */
    @Query("SELECT c.category, COUNT(c) FROM Complaint c GROUP BY c.category")
    List<Object[]> countGroupedByCategory();

    /** Returns [priority, count] pairs. */
    @Query("SELECT c.priority, COUNT(c) FROM Complaint c GROUP BY c.priority")
    List<Object[]> countGroupedByPriority();

    /**
     * Average days from complaint creation to resolution.
     * Returns null if no complaints have been resolved yet.
     * Uses a native PostgreSQL query because JPQL does not support EXTRACT(EPOCH FROM interval).
     */
    @Query(value = """
        SELECT AVG(
            EXTRACT(EPOCH FROM (cr.resolved_at - c.created_at)) / 86400.0
        )
        FROM complaints c
        JOIN complaint_resolutions cr ON cr.complaint_id = c.id
        WHERE c.status = 'RESOLVED'
    """, nativeQuery = true)
    Double averageResolutionDays();
}

