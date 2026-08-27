package com.ethiorental.backend.property.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record PropertyUnitRequest(
        String unitCode,
        String unitName,
        String unitType,
        @DecimalMin("0.0") BigDecimal areaSqMeter,
        String status,
        @DecimalMin("0.0") BigDecimal rentAmount,
        String tenantName,
        String floorLevel,
        String category,
        String shopNumber,
        Boolean submeter,
        Boolean waterSupply,
        String frontage,
        String description
) {
    public PropertyUnitRequest {
        if (status == null || status.isBlank()) {
            status = "AVAILABLE";
        }
    }
}
