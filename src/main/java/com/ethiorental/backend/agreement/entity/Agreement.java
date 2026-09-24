package com.ethiorental.backend.agreement.entity;

import com.ethiorental.backend.IAM.entity.Citizen;
import com.ethiorental.backend.agreement.enums.AgreementStatus;
import com.ethiorental.backend.property.entity.Property;
import com.ethiorental.backend.property.entity.PropertyUnit;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "agreements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Agreement {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(unique = true, nullable = false, updatable = false)
    private String agreementNumber;

    @Column(unique = true, nullable = false, updatable = false)
    private String requestCode;

    @Column
    private UUID leaseRequestId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_id")
    private PropertyUnit unit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Citizen tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "landlord_id", nullable = false)
    private Citizen landlord;

    @Column(nullable = false)
    private BigDecimal monthlyRent;

    @Column
    private Integer advancePaymentMonths;

    @Column(nullable = false)
    private Integer leaseDurationMonths;

    @Column
    private LocalDate contractDate;

    @Column
    private LocalDateTime startDate;

    @Column
    private LocalDateTime endDate;

    @Column
    private Integer monthlyPaymentDueDay;

    @Column
    private String utilitiesPaidBy;

    @Column
    private String propertyCondition;

    @Column
    private String propertyOwnershipType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AgreementStatus status;

    @Builder.Default
    @Column
    private Boolean landlordSigned = false;

    @Column
    private LocalDateTime landlordSignedAt;

    @Column
    private String landlordSignature;

    @Builder.Default
    @Column
    private Boolean tenantSigned = false;

    @Column
    private LocalDateTime tenantSignedAt;

    @Column
    private String tenantSignature;

    @Builder.Default
    @Column
    private Boolean officerVerified = false;

    @Column
    private LocalDateTime officerVerifiedAt;

    @Column
    private String officerEmail;

    @Builder.Default
    @Column
    private Boolean supervisorApproved = false;

    @Column
    private LocalDateTime supervisorApprovedAt;

    @Column
    private String supervisorEmail;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;

    @Column
    private LocalDateTime cancellationRequestedAt;

    @Builder.Default
    @Column
    private Boolean cancellationRequestedByLandlord = false;

    @Builder.Default
    @Column
    private Integer totalMonthsPaid = 0;

    @Column
    private LocalDate paidThroughDate;

    @Column
    private LocalDate nextPaymentDueDate;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) this.status = AgreementStatus.ACTIVE;
        if (this.totalMonthsPaid == null) this.totalMonthsPaid = 0;
        if (this.startDate != null) {
            if (this.monthlyPaymentDueDay == null) {
                this.monthlyPaymentDueDay = this.startDate.getDayOfMonth();
            }
            if (this.nextPaymentDueDate == null) {
                this.nextPaymentDueDate = this.startDate.toLocalDate();
            }
        } else if (this.monthlyPaymentDueDay == null) {
            this.monthlyPaymentDueDay = 5;
        }
        if (this.agreementNumber == null) {
            this.agreementNumber = "AGR" + System.currentTimeMillis() + (int)(Math.random() * 1000);
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
