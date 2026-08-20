package com.ethiorental.backend.complaint.entity;

import com.ethiorental.backend.IAM.entity.GovernmentEmployee;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Records the final resolution of a complaint.
 * One-to-one with {@link Complaint}; absent until the complaint is resolved.
 */
@Entity
@Table(name = "complaint_resolutions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ComplaintResolution {

    @Id
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "complaint_id", nullable = false, unique = true)
    private Complaint complaint;

    /** Officer who recorded the resolution. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resolved_by_id", nullable = false)
    private GovernmentEmployee resolvedBy;

    /** Human-readable summary of what action was taken. */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String resolutionSummary;

    /**
     * Outcome code — plain-text for now; could later be an enum
     * (ACTION_TAKEN, NO_VIOLATION_FOUND, REFERRED_TO_COURT, …).
     */
    @Column(nullable = false)
    private String outcome;

    @Column(nullable = false, updatable = false)
    private LocalDateTime resolvedAt;

    @PrePersist
    protected void onCreate() {
        this.resolvedAt = LocalDateTime.now();
    }
}
