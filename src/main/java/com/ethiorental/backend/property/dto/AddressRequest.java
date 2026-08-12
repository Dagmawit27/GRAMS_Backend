package com.ethiorental.backend.property.dto;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record AddressRequest(
        @NotBlank String city,
        @NotBlank String subCity,
        @NotBlank String woreda,
        String kebele,
        String street,
        String houseNumber,
        BigDecimal latitude,
        BigDecimal longitude
) {}
