package com.ethiorental.backend.lease.controller;

import com.ethiorental.backend.lease.dto.LeaseRequestRequest;
import com.ethiorental.backend.lease.dto.LeaseRequestResponse;
import com.ethiorental.backend.lease.dto.LeaseStatusUpdateRequest;
import com.ethiorental.backend.lease.enums.LeaseRequestStatus;
import com.ethiorental.backend.lease.service.LeaseRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import com.ethiorental.backend.IAM.entity.GovernmentEmployee;
import com.ethiorental.backend.IAM.repository.GovernmentEmployeeRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/lease-requests")
@RequiredArgsConstructor
public class LeaseRequestController {

    private final LeaseRequestService leaseRequestService;
    private final GovernmentEmployeeRepository governmentEmployeeRepository;

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

    /**
     * Sign agreement using password - landlord or tenant.
     */
    @PostMapping("/{requestCode}/sign-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LeaseRequestResponse> signAgreementWithPassword(
            @PathVariable String requestCode,
            @RequestBody String password,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        return ResponseEntity.ok(leaseRequestService.signAgreementWithPassword(requestCode, password, userDetails.getUsername()));
    }

    /**
     * Sign agreement using OTP - landlord or tenant.
     */
    @PostMapping("/{requestCode}/sign-otp")
    @PreAuthorize("hasAnyRole('LANDLORD','CITIZEN','BOTH')")
    public ResponseEntity<LeaseRequestResponse> signAgreementWithOtp(
            @PathVariable String requestCode,
            @RequestBody String otp,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        return ResponseEntity.ok(leaseRequestService.signAgreementWithOtp(requestCode, otp, userDetails.getUsername()));
    }

    /**
     * Verify lease request - officer only.
     */
    @PostMapping("/{requestCode}/verify")
    @PreAuthorize("hasAnyRole('WOREDA_OFFICER','GOVERNMENT_EMPLOYEE')")
    public ResponseEntity<LeaseRequestResponse> verifyLeaseRequest(
            @PathVariable String requestCode,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        return ResponseEntity.ok(leaseRequestService.verifyLeaseRequest(requestCode, userDetails.getUsername()));
    }

    /**
     * Approve lease request - supervisor only.
     */
    @PostMapping("/{requestCode}/approve")
    @PreAuthorize("hasAnyRole('WOREDA_SUPERVISOR','GOVERNMENT_EMPLOYEE')")
    public ResponseEntity<LeaseRequestResponse> approveLeaseRequest(
            @PathVariable String requestCode,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        return ResponseEntity.ok(leaseRequestService.approveLeaseRequest(requestCode, userDetails.getUsername()));
    }

    /**
     * Get lease requests by status - filtered by officer's assigned jurisdiction.
     */
    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('WOREDA_OFFICER','WOREDA_SUPERVISOR','SUB_CITY_ADMINISTRATOR','CITY_ADMINISTRATOR','GOVERNMENT_EMPLOYEE')")
    public ResponseEntity<List<LeaseRequestResponse>> getLeaseRequestsByStatus(
            @PathVariable String status,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        LeaseRequestStatus leaseRequestStatus = LeaseRequestStatus.fromString(status);

        if (userDetails != null && userDetails.getUsername() != null) {
            String email = userDetails.getUsername();
            Optional<GovernmentEmployee> employeeOpt = governmentEmployeeRepository.findByEmail(email);
            if (employeeOpt.isPresent()) {
                GovernmentEmployee emp = employeeOpt.get();
                if (emp.getOffice() != null) {
                    String officeType = emp.getOffice().getOfficeType();
                    // City level / HEAD_OFFICE sees all
                    if ("HEAD_OFFICE".equalsIgnoreCase(officeType)) {
                        return ResponseEntity.ok(leaseRequestService.getLeaseRequestsByStatus(leaseRequestStatus));
                    }
                    String subCity = emp.getOffice().getSubCity();
                    String woreda = emp.getOffice().getWoreda();
                    if (subCity != null && !subCity.isBlank() && woreda != null && !woreda.isBlank()) {
                        return ResponseEntity.ok(
                                leaseRequestService.getLeaseRequestsByStatusAndJurisdiction(leaseRequestStatus, subCity, woreda));
                    }
                }
            }
        }
        
        return ResponseEntity.ok(leaseRequestService.getLeaseRequestsByStatus(leaseRequestStatus));
    }
}
