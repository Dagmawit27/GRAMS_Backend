package com.ethiorental.backend.property.service;

import com.ethiorental.backend.property.dto.PropertyRequest;
import com.ethiorental.backend.property.dto.PropertyResponse;
import com.ethiorental.backend.property.dto.PropertyUnitResponse;
import com.ethiorental.backend.property.enums.PropertyStatus;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface PropertyService {

    /** Register a new property. Only citizens with LANDLORD role can call this. */
    PropertyResponse registerProperty(PropertyRequest request,
                                      List<MultipartFile> images,
                                      List<MultipartFile> ownershipDocuments,
                                      String landlordEmail);

    /** Get all properties owned by the authenticated landlord. */
    List<PropertyResponse> getMyProperties(String landlordEmail);

    /** Get a single property by id (owner or officer). */
    PropertyResponse getPropertyById(UUID id);

    /** Get all properties filtered by status (public/officer facing). */
    List<PropertyResponse> getPropertiesByStatus(PropertyStatus status);

    /** Get properties within a specific sub-city + woreda jurisdiction for officers. */
    List<PropertyResponse> getPropertiesByJurisdiction(String subCity, String woreda, PropertyStatus status);

    /** Update property status — used by officers during the review workflow. */
    PropertyResponse updatePropertyStatus(UUID id, PropertyStatus newStatus, String remarks, String officerUsername);

    /** Delete a property — only by owner and only if status is PENDING. */
    void deleteProperty(UUID id, String landlordEmail);

    /** Update a property — only by owner and only if status is PENDING. */
    PropertyResponse updateProperty(UUID id, PropertyRequest request, List<MultipartFile> images, List<MultipartFile> ownershipDocuments, String landlordEmail);

    /** Get all units for a specific property. */
    List<PropertyUnitResponse> getPropertyUnits(UUID propertyId);

    /** Get a single unit by id. */
    PropertyUnitResponse getUnitById(UUID unitId);

    /** Add units to a property. */
    List<PropertyUnitResponse> addUnitsToProperty(UUID propertyId, List<PropertyUnitResponse> units, String landlordEmail);

    /** Update unit status (e.g., rent out a unit). */
    PropertyUnitResponse updateUnitStatus(UUID unitId, String newStatus, String tenantName, String landlordEmail);

    /** Delete a unit from a property. */
    void deleteUnit(UUID unitId, String landlordEmail);
}
