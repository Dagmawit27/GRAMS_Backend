package com.ethiorental.backend.agreement.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AgreementResponse(
        Long id,
        String agreementCode,
        String requestCode,
        String contractDate,
        String contractNumber,
        
        // Landlord Information
        String landlordName,
        String landlordSubCity,
        String landlordWoreda,
        String landlordHouseNo,
        String landlordPhone,
        String landlordRegion,
        String landlordCity,
        String landlordSpecificPlace,
        
        // Tenant Information
        String tenantName,
        String tenantSubCity,
        String tenantWoreda,
        String tenantHouseNo,
        String tenantPhone,
        String tenantRegion,
        String tenantCity,
        String tenantSpecificPlace,
        
        // Property Information
        String propertyRegion,
        String propertyCity,
        String propertySubCity,
        String propertyWoreda,
        String propertySpecificPlace,
        String propertyHouseNo,
        String propertyOwnershipType,
        
        // Rental Conditions
        String propertyCondition,
        BigDecimal monthlyRentInBirr,
        String monthlyRentInWords,
        String utilitiesPaidBy,
        
        // Payment Terms
        String advancePaymentMonths,
        BigDecimal advancePaymentBirr,
        String advancePaymentWords,
        String monthlyPaymentDueDay,
        
        // Signatures
        String landlordSignature,
        LocalDateTime landlordSignedAt,
        String tenantSignature,
        LocalDateTime tenantSignedAt,
        String officerName,
        String officerSignature,
        LocalDateTime officerSignedAt,
        String witness1Name,
        String witness1Signature,
        LocalDateTime witness1SignedAt,
        String witness2Name,
        String witness2Signature,
        LocalDateTime witness2SignedAt,
        
        // Status
        boolean landlordSigned,
        boolean tenantSigned,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
