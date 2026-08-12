package com.ethiorental.backend.property.dto;

import com.ethiorental.backend.property.enums.PropertyStatus;
import jakarta.validation.constraints.NotNull;

public record StatusUpdateRequest(
        @NotNull PropertyStatus status,
        String remarks
) {}
