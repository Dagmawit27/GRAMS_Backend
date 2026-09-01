package com.ethiorental.backend.agreement.entity;

import com.ethiorental.backend.IAM.entity.Citizen;
import com.ethiorental.backend.lease.entity.LeaseRequest;
import com.ethiorental.backend.property.entity.Property;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "agreements")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Agreement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String agreementCode;

    @Column(nullable = false)
    private String contractDate;

    @Column(nullable = false)
    private String contractNumber;

    // Lease Request Reference
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lease_request_id", nullable = false)
    private LeaseRequest leaseRequest;

    @Column(nullable = false)
    private String requestCode;

    // Landlord Information
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "landlord_id", nullable = false)
    private Citizen landlord;

    @Column(nullable = false)
    private String landlordName;

    @Column(nullable = false)
    private String landlordSubCity;

    @Column(nullable = false)
    private String landlordWoreda;

    @Column(nullable = false)
    private String landlordHouseNo;

    @Column(nullable = false)
    private String landlordPhone;

    @Column(nullable = false)
    private String landlordRegion;

    @Column(nullable = false)
    private String landlordCity;

    @Column(nullable = false)
    private String landlordSpecificPlace;

    // Tenant Information
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Citizen tenant;

    @Column(nullable = false)
    private String tenantName;

    @Column(nullable = false)
    private String tenantSubCity;

    @Column(nullable = false)
    private String tenantWoreda;

    @Column(nullable = false)
    private String tenantHouseNo;

    @Column(nullable = false)
    private String tenantPhone;

    @Column(nullable = false)
    private String tenantRegion;

    @Column(nullable = false)
    private String tenantCity;

    @Column(nullable = false)
    private String tenantSpecificPlace;

    // Property Information
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @Column(nullable = false)
    private String propertyRegion;

    @Column(nullable = false)
    private String propertyCity;

    @Column(nullable = false)
    private String propertySubCity;

    @Column(nullable = false)
    private String propertyWoreda;

    @Column(nullable = false)
    private String propertySpecificPlace;

    @Column(nullable = false)
    private String propertyHouseNo;

    @Column(nullable = false)
    private String propertyOwnershipType;

    // Rental Conditions
    @Column(nullable = false)
    private String propertyCondition;

    @Column(nullable = false)
    private BigDecimal monthlyRentInBirr;

    @Column(nullable = false)
    private String monthlyRentInWords;

    @Column(nullable = false)
    private String utilitiesPaidBy;

    // Payment Terms
    @Column(nullable = false)
    private String advancePaymentMonths;

    @Column(nullable = false)
    private BigDecimal advancePaymentBirr;

    @Column(nullable = false)
    private String advancePaymentWords;

    @Column(nullable = false)
    private String monthlyPaymentDueDay;

    // Signatures
    @Column
    private String landlordSignature;

    @Column
    private LocalDateTime landlordSignedAt;

    @Column
    private String tenantSignature;

    @Column
    private LocalDateTime tenantSignedAt;

    @Column
    private String officerName;

    @Column
    private String officerSignature;

    @Column
    private LocalDateTime officerSignedAt;

    @Column
    private String witness1Name;

    @Column
    private String witness1Signature;

    @Column
    private LocalDateTime witness1SignedAt;

    @Column
    private String witness2Name;

    @Column
    private String witness2Signature;

    @Column
    private LocalDateTime witness2SignedAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private boolean landlordSigned = false;

    @Column(nullable = false)
    private boolean tenantSigned = false;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
