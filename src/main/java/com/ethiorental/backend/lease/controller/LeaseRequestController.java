package com.ethiorental.backend.lease.controller;

import com.ethiorental.backend.lease.dto.LeaseRequestRequest;
import com.ethiorental.backend.lease.dto.LeaseRequestResponse;
import com.ethiorental.backend.lease.dto.LeaseStatusUpdateRequest;
import com.ethiorental.backend.lease.service.LeaseRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/lease-requests")
@RequiredArgsConstructor
public class LeaseRequestController {

    private final LeaseRequestService leaseRequestService;

    /**
     * Submit a new lease application for a property or unit.
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LeaseRequestResponse> submitLeaseRequest(
            @RequestBody @Valid LeaseRequestRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        LeaseRequestResponse response = leaseRequestService.submitLeaseRequest(request, userDetails.getUsername());
        return ResponseEntity.status(201).body(response);
    }

    /**
     * Get all lease requests for the authenticated applicant.
     */
    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<LeaseRequestResponse>> getMyLeaseRequests(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        return ResponseEntity.ok(leaseRequestService.getMyLeaseRequests(userDetails.getUsername()));
    }

    /**
     * Get all lease requests for a landlord's properties.
     */
    @GetMapping("/landlord")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<LeaseRequestResponse>> getLandlordLeaseRequests(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        return ResponseEntity.ok(leaseRequestService.getLandlordLeaseRequests(userDetails.getUsername()));
    }

    /**
     * Get a single lease request by request code.
     */
    @GetMapping("/{requestCode}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LeaseRequestResponse> getLeaseRequestById(@PathVariable String requestCode) {
        return ResponseEntity.ok(leaseRequestService.getLeaseRequestByCode(requestCode));
    }

    /**
     * Update lease request status (approve/reject) - landlord only.
     */
    @PatchMapping("/{requestCode}/status")
    @PreAuthorize("hasAnyRole('LANDLORD','CITIZEN','BOTH')")
    public ResponseEntity<LeaseRequestResponse> updateLeaseRequestStatus(
            @PathVariable String requestCode,
            @RequestBody @Valid LeaseStatusUpdateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        return ResponseEntity.ok(leaseRequestService.updateLeaseRequestStatus(requestCode, request, userDetails.getUsername()));
    }

    /**
     * Cancel a lease request - applicant only.
     */
    @PatchMapping("/{requestCode}/cancel")
    @PreAuthorize("hasAnyRole('TENANT','CITIZEN','BOTH')")
    public ResponseEntity<Void> cancelLeaseRequest(
            @PathVariable String requestCode,
            @AuthenticationPrincipal UserDetails userDetails) {

        leaseRequestService.cancelLeaseRequest(requestCode, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    /**
     * Delete a cancelled lease request - applicant only.
     */
    @DeleteMapping("/{requestCode}")
    @PreAuthorize("hasAnyRole('TENANT','CITIZEN','BOTH')")
    public ResponseEntity<Void> deleteLeaseRequest(
            @PathVariable String requestCode,
            @AuthenticationPrincipal UserDetails userDetails) {

        leaseRequestService.deleteLeaseRequest(requestCode, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    /**
     * Get pending lease requests for a specific property.
     */
    @GetMapping("/property/{propertyId}/pending")
    @PreAuthorize("hasAnyRole('LANDLORD','CITIZEN','BOTH')")
    public ResponseEntity<List<LeaseRequestResponse>> getPendingRequestsForProperty(
            @PathVariable UUID propertyId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        return ResponseEntity.ok(leaseRequestService.getPendingRequestsForProperty(propertyId, userDetails.getUsername()));
    }

    /**
     * Get pending lease requests for a specific unit.
     */
    @GetMapping("/unit/{unitId}/pending")
    @PreAuthorize("hasAnyRole('LANDLORD','CITIZEN','BOTH')")
    public ResponseEntity<List<LeaseRequestResponse>> getPendingRequestsForUnit(
            @PathVariable UUID unitId,
            @AuthenticationPrincipal UserDetails userDetails) {

        return ResponseEntity.ok(leaseRequestService.getPendingRequestsForUnit(unitId, userDetails.getUsername()));
    }
}
