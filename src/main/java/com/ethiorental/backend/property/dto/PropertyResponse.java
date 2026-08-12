package com.ethiorental.backend.property.dto;

import com.ethiorental.backend.property.enums.PropertyStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record PropertyResponse(
        UUID id,
        String propertyCode,
        String propertyType,
        AddressResponse address,
        String houseNumber,
        String floorNumber,
        Integer bedroomCount,
        Integer bathroomCount,
        BigDecimal areaSqMeter,
        BigDecimal monthlyRent,
        String furnishingStatus,
        String description,
        PropertyStatus status,
        UUID landlordId,
        List<PropertyImageResponse> images,
        List<OwnershipDocumentResponse> ownershipDocuments,
        LocalDateTime createdAt
) {}
