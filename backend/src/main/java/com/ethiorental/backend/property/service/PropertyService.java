package com.ethiorental.backend.property.service;

import com.ethiorental.backend.property.dto.PropertyRequest;
import com.ethiorental.backend.property.dto.PropertyResponse;
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
}
