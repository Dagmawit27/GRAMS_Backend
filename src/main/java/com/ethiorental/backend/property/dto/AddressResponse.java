package com.ethiorental.backend.property.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record AddressResponse(
        UUID id,
        String city,
        String subCity,
        String woreda,
        String kebele,
        String street,
        String houseNumber,
        BigDecimal latitude,
        BigDecimal longitude
) {}
