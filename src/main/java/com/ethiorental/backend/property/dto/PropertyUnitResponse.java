package com.ethiorental.backend.property.dto;

import com.ethiorental.backend.property.enums.UnitStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PropertyUnitResponse(
        UUID id,
        UUID propertyId,
        String unitCode,
        String unitName,
        String unitType,
        BigDecimal areaSqMeter,
        UnitStatus status,
        BigDecimal rentAmount,
        String tenantName,
        String floorLevel,
        String category,
        Boolean submeter,
        Boolean waterSupply,
        String frontage,
        String description,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
