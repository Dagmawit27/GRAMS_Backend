package com.ethiorental.backend.agreement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgreementResponse {

    private UUID id;
    private String agreementNumber;
    private String requestCode;
    private UUID leaseRequestId;

    // Property references & flattened details
    private UUID propertyId;
    private String propertyCode;
    private String propertyTitle;
    private String propertyType;
    private String propertySubCity;
    private String propertyWoreda;
    private UUID unitId;
    private String unitCode;
    private String unitNumber;

    // Tenant references & flattened details
    private UUID tenantId;
    private String tenantName;
    private String tenantEmail;
    private String tenantPhone;
    private String tenantSubCity;
    private String tenantWoreda;

    // Landlord references & flattened details
    private UUID landlordId;
    private String landlordName;
    private String landlordEmail;
    private String landlordPhone;
    private String landlordSubCity;
    private String landlordWoreda;
    private String landlordPreferredPaymentMethod;
    private String landlordBankName;
    private String landlordAccountNumber;
    private String landlordAccountHolderName;
    private String landlordBankName2;
    private String landlordAccountNumber2;
    private String landlordAccountHolderName2;
    private String landlordBankName3;
    private String landlordAccountNumber3;
    private String landlordAccountHolderName3;
    private String landlordTinNumber;

    // Contract terms
    private BigDecimal monthlyRent;
    private BigDecimal securityDeposit;
    private Integer advancePaymentMonths;
    private Integer leaseDurationMonths;
    private LocalDate contractDate;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Integer monthlyPaymentDueDay;
    private String utilitiesPaidBy;
    private String propertyCondition;
    private String propertyOwnershipType;
    private String status;

    // Signatures & verification
    private Boolean landlordSigned;
    private LocalDateTime landlordSignedAt;
    private String landlordSignature;
    private Boolean tenantSigned;
    private LocalDateTime tenantSignedAt;
    private String tenantSignature;
    private Boolean officerVerified;
    private LocalDateTime officerVerifiedAt;
    private String officerEmail;
    private Boolean supervisorApproved;
    private LocalDateTime supervisorApprovedAt;
    private String supervisorEmail;

    // Payment state tracking
    private Integer totalMonthsPaid;
    private LocalDate paidThroughDate;
    private LocalDate nextPaymentDueDate;

    // Cancellation
    private LocalDateTime cancellationRequestedAt;
    private Boolean cancellationRequestedByLandlord;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
