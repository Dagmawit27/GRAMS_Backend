package com.ethiorental.backend.lease.entity;

import com.ethiorental.backend.IAM.entity.Citizen;
import com.ethiorental.backend.lease.enums.LeaseRequestStatus;
import com.ethiorental.backend.property.entity.Property;
import com.ethiorental.backend.property.entity.PropertyUnit;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "lease_requests")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LeaseRequest {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(unique = true, nullable = false, updatable = false)
    private String requestCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_id")
    private PropertyUnit unit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applicant_id", nullable = false)
    private Citizen applicant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "landlord_id", nullable = false)
    private Citizen landlord;

    @Column(nullable = false)
    private BigDecimal proposedRent;

    @Column(nullable = false)
    private Integer leaseDurationMonths;

    @Column(columnDefinition = "TEXT")
    private String applicantNotes;

    @Column(columnDefinition = "TEXT")
    private String landlordRemarks;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LeaseRequestStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime reviewedAt;

    @Column
    private LocalDateTime expiresAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) this.status = LeaseRequestStatus.PENDING;
        // generate request code: LR + timestamp + random
        this.requestCode = "LR" + System.currentTimeMillis() + (int)(Math.random() * 1000);
    }
}
