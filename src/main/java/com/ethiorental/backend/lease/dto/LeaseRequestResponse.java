package com.ethiorental.backend.lease.dto;

import com.ethiorental.backend.lease.enums.LeaseRequestStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record LeaseRequestResponse(
        UUID id,
        UUID propertyId,
        String propertyCode,
        String propertyTitle,
        String propertyType,
        String propertyLocation,
        String propertyImage,
        List<String> propertyImages,
        UUID unitId,
        String unitCode,
        String unitNumber,
        BigDecimal area,
        UUID applicantId,
        String applicantName,
        String applicantEmail,
        String applicantPhone,
        String applicantNationalId,
        String applicantEmployment,
        UUID landlordId,
        String landlordName,
        String landlordEmail,
        BigDecimal proposedRent,
        BigDecimal securityDeposit,
        Integer leaseDurationMonths,
        String startDate,
        String endDate,
        String applicantNotes,
        String landlordRemarks,
        LeaseRequestStatus status,
        LocalDateTime createdAt,
        LocalDateTime reviewedAt,
        LocalDateTime expiresAt
) {}
