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
        String title,
        AddressResponse address,
        String houseNumber,
        String floorNumber,
        Integer bedroomCount,
        Integer bathroomCount,
        BigDecimal areaSqMeter,
        BigDecimal monthlyRent,
        String furnishingStatus,
        String description,
        String ownershipType,
        String specificLandmark,
        String cadastralParcelId,
        String titleDeedNumber,
        Integer securityDepositMonths,
        String minLeasePeriod,
        String availableFrom,
        PropertyStatus status,
        UUID landlordId,
        String landlordName,
        String landlordPhone,
        String landlordEmail,
        List<PropertyImageResponse> images,
        List<OwnershipDocumentResponse> ownershipDocuments,
        List<PropertyUnitResponse> units,
        LocalDateTime createdAt
) {}
