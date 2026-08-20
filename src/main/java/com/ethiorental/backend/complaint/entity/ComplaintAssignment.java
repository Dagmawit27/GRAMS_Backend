package com.ethiorental.backend.complaint.entity;

import com.ethiorental.backend.IAM.entity.GovernmentEmployee;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Records which government officer was assigned to investigate a complaint
 * and when. Multiple assignment rows may exist per complaint (e.g. reassignments).
 * The most recent active record is the current assignee.
 */
@Entity
@Table(name = "complaint_assignments")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ComplaintAssignment {

    @Id
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "complaint_id", nullable = false)
    private Complaint complaint;

    /** Officer who was assigned. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assigned_officer_id", nullable = false)
    private GovernmentEmployee assignedOfficer;

    /** Employee who performed the assignment (supervisor, admin, etc.). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assigned_by_id", nullable = false)
    private GovernmentEmployee assignedBy;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(nullable = false, updatable = false)
    private LocalDateTime assignedAt;

    /** Set when this assignment is superseded by a later reassignment. */
    private LocalDateTime revokedAt;

    @PrePersist
    protected void onCreate() {
        this.assignedAt = LocalDateTime.now();
    }
}
