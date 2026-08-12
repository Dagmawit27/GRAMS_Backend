package com.ethiorental.backend.property.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record PropertyRequest(
        @NotBlank String propertyType,
        @Valid @NotNull AddressRequest address,
        String houseNumber,
        String floorNumber,
        @Min(0) Integer bedroomCount,
        @Min(0) Integer bathroomCount,
        @DecimalMin("0.0") BigDecimal areaSqMeter,
        @NotNull @DecimalMin("0.0") BigDecimal monthlyRent,
        String furnishingStatus,
        String description
) {}
