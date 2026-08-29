package com.ethiorental.backend.lease.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.UUID;

public record LeaseRequestRequest(
        @NotNull UUID propertyId,
        UUID unitId,
        @NotNull @DecimalMin("0.0") BigDecimal proposedRent,
        @NotNull @Min(1) @Max(120) Integer leaseDurationMonths,
        @Size(max = 1000) String applicantNotes
) {}
