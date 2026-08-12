package com.ethiorental.backend.property.mapper;

import com.ethiorental.backend.property.dto.*;
import com.ethiorental.backend.property.entity.*;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class PropertyMapper {

    public Address toAddressEntity(AddressRequest dto) {
        return Address.builder()
                .city(dto.city())
                .subCity(dto.subCity())
                .woreda(dto.woreda())
                .kebele(dto.kebele())
                .street(dto.street())
                .houseNumber(dto.houseNumber())
                .latitude(dto.latitude())
                .longitude(dto.longitude())
                .build();
    }

    public AddressResponse toAddressResponse(Address a) {
        return new AddressResponse(
                a.getId(), a.getCity(), a.getSubCity(), a.getWoreda(),
                a.getKebele(), a.getStreet(), a.getHouseNumber(),
                a.getLatitude(), a.getLongitude()
        );
    }

    public PropertyResponse toPropertyResponse(Property p) {
        List<PropertyImageResponse> images = p.getImages() == null
                ? Collections.emptyList()
                : p.getImages().stream().map(this::toImageResponse).toList();

        List<OwnershipDocumentResponse> docs = p.getOwnershipDocuments() == null
                ? Collections.emptyList()
                : p.getOwnershipDocuments().stream().map(this::toDocResponse).toList();

        return new PropertyResponse(
                p.getId(),
                p.getPropertyCode(),
                p.getPropertyType(),
                toAddressResponse(p.getAddress()),
                p.getHouseNumber(),
                p.getFloorNumber(),
                p.getBedroomCount(),
                p.getBathroomCount(),
                p.getAreaSqMeter(),
                p.getMonthlyRent(),
                p.getFurnishingStatus(),
                p.getDescription(),
                p.getStatus(),
                p.getLandlord().getId(),
                images,
                docs,
                p.getCreatedAt()
        );
    }

    public PropertyImageResponse toImageResponse(PropertyImage img) {
        return new PropertyImageResponse(
                img.getId(), img.getImageUrl(), img.isCover(), img.getUploadedAt()
        );
    }

    public OwnershipDocumentResponse toDocResponse(OwnershipDocument doc) {
        return new OwnershipDocumentResponse(
                doc.getId(), doc.getDocumentNumber(), doc.getDocumentType(),
                doc.getFilePath(), doc.getIssueDate(), doc.getExpiryDate()
        );
    }
}
