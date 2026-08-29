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
    @PreAuthorize("hasAnyRole('LANDLORD','CITIZEN','BOTH')")
    public ResponseEntity<PropertyResponse> registerProperty(
            @RequestPart("property") @Valid PropertyRequest request,
            @RequestParam(value = "images", required = false) List<MultipartFile> images,
            @RequestParam(value = "documents", required = false) List<MultipartFile> ownershipDocuments,
            @AuthenticationPrincipal UserDetails userDetails) {

        PropertyResponse response = propertyService.registerProperty(
                request, images, ownershipDocuments, userDetails.getUsername());
        return ResponseEntity.status(201).body(response);
    }

    /**
     * Get all properties belonging to the authenticated landlord.
     */
    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('LANDLORD','CITIZEN','BOTH')")
    public ResponseEntity<List<PropertyResponse>> getMyProperties(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(propertyService.getMyProperties(userDetails.getUsername()));
    }

    /**
     * Get a single property by id — any authenticated user (officer, supervisor, citizen).
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PropertyResponse> getPropertyById(@PathVariable UUID id) {
        return ResponseEntity.ok(propertyService.getPropertyById(id));
    }

    /**
     * Get a single property by property code — any authenticated user.
     */
    @GetMapping("/code/{propertyCode}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PropertyResponse> getPropertyByCode(@PathVariable String propertyCode) {
        return ResponseEntity.ok(propertyService.getPropertyByCode(propertyCode));
    }

    /**
     * Officer/Supervisor: get a single property by id for review purposes.
     */
    @GetMapping("/officer/detail/{id}")
    @PreAuthorize("hasAnyRole('WOREDA_OFFICER','WOREDA_SUPERVISOR','SUB_CITY_ADMINISTRATOR','CITY_ADMINISTRATOR','SYSTEM_ADMINISTRATOR')")
    public ResponseEntity<PropertyResponse> getPropertyForOfficer(@PathVariable UUID id) {
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
     * Get properties for a specific sub-city + woreda — used by officers/supervisors
     * to see only properties within their jurisdiction.
     */
    @GetMapping("/jurisdiction")
    @PreAuthorize("hasAnyRole('WOREDA_OFFICER','WOREDA_SUPERVISOR','SUB_CITY_ADMINISTRATOR','CITY_ADMINISTRATOR','SYSTEM_ADMINISTRATOR')")
    public ResponseEntity<List<PropertyResponse>> getPropertiesByJurisdiction(
            @RequestParam String subCity,
            @RequestParam String woreda,
            @RequestParam(defaultValue = "PENDING") PropertyStatus status) {
        return ResponseEntity.ok(propertyService.getPropertiesByJurisdiction(subCity, woreda, status));
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

    /**
     * Delete a property — only property owner (LANDLORD) and only if status is PENDING.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('LANDLORD','CITIZEN','BOTH')")
    public ResponseEntity<Void> deleteProperty(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        propertyService.deleteProperty(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    /**
     * Update a property — only property owner (LANDLORD) and only if status is PENDING.
     */
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('LANDLORD','CITIZEN','BOTH')")
    public ResponseEntity<PropertyResponse> updateProperty(
            @PathVariable UUID id,
            @RequestPart("property") @Valid PropertyRequest request,
            @RequestParam(value = "images", required = false) List<MultipartFile> images,
            @RequestParam(value = "documents", required = false) List<MultipartFile> ownershipDocuments,
            @AuthenticationPrincipal UserDetails userDetails) {
        PropertyResponse response = propertyService.updateProperty(
                id, request, images, ownershipDocuments, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }
}
