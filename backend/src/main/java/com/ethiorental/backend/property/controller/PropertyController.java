package com.ethiorental.backend.property.controller;

import com.ethiorental.backend.property.dto.PropertyRequest;
import com.ethiorental.backend.property.dto.PropertyResponse;
import com.ethiorental.backend.property.dto.StatusUpdateRequest;
import com.ethiorental.backend.property.enums.PropertyStatus;
import com.ethiorental.backend.property.service.PropertyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/properties")
@RequiredArgsConstructor
public class PropertyController {

    private final PropertyService propertyService;

    /**
     * Register a new property — LANDLORD only.
     * Accepts multipart/form-data with JSON part + file parts.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('LANDLORD')")
    public ResponseEntity<PropertyResponse> registerProperty(
            @RequestPart("property") @Valid PropertyRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            @RequestPart(value = "documents", required = false) List<MultipartFile> ownershipDocuments,
            @AuthenticationPrincipal UserDetails userDetails) {

        PropertyResponse response = propertyService.registerProperty(
                request, images, ownershipDocuments, userDetails.getUsername());
        return ResponseEntity.status(201).body(response);
    }

    /**
     * Get all properties belonging to the authenticated landlord.
     */
    @GetMapping("/my")
    @PreAuthorize("hasRole('LANDLORD')")
    public ResponseEntity<List<PropertyResponse>> getMyProperties(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(propertyService.getMyProperties(userDetails.getUsername()));
    }

    /**
     * Get a single property by id — authenticated users.
     */
    @GetMapping("/{id}")
    public ResponseEntity<PropertyResponse> getPropertyById(@PathVariable UUID id) {
        return ResponseEntity.ok(propertyService.getPropertyById(id));
    }

    /**
     * Get properties filtered by status — e.g. ?status=LISTED for public browsing,
     * or ?status=PENDING for officer review.
     */
    @GetMapping
    public ResponseEntity<List<PropertyResponse>> getPropertiesByStatus(
            @RequestParam(defaultValue = "LISTED") PropertyStatus status) {
        return ResponseEntity.ok(propertyService.getPropertiesByStatus(status));
    }

    /**
     * Update property status — WOREDA_OFFICER or WOREDA_SUPERVISOR only.
     * Officer: PENDING → VERIFIED or REJECTED
     * Supervisor: VERIFIED → LISTED or REJECTED (suspended)
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('WOREDA_OFFICER','WOREDA_SUPERVISOR','SUB_CITY_ADMINISTRATOR','CITY_ADMINISTRATOR','SYSTEM_ADMINISTRATOR')")
    public ResponseEntity<PropertyResponse> updatePropertyStatus(
            @PathVariable UUID id,
            @RequestBody @Valid StatusUpdateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(propertyService.updatePropertyStatus(
                id, request.status(), request.remarks(), userDetails.getUsername()));
    }
}
