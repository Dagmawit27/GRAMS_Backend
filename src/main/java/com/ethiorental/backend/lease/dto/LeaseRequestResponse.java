package com.ethiorental.backend.lease.dto;

import com.ethiorental.backend.lease.enums.LeaseRequestStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record LeaseRequestResponse(
        UUID id,
        UUID propertyId,
        String propertyCode,
        String propertyTitle,
        UUID unitId,
        String unitCode,
        UUID applicantId,
        String applicantName,
        String applicantEmail,
        UUID landlordId,
        String landlordName,
        String landlordEmail,
        BigDecimal proposedRent,
        Integer leaseDurationMonths,
        String applicantNotes,
        String landlordRemarks,
        LeaseRequestStatus status,
        LocalDateTime createdAt,
        LocalDateTime reviewedAt,
        LocalDateTime expiresAt
) {}
