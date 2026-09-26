package com.ethiorental.backend.agreement.controller;

import com.ethiorental.backend.agreement.dto.AgreementResponse;
import com.ethiorental.backend.agreement.service.AgreementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/v1/agreements", "/api/agreements"})
@RequiredArgsConstructor
public class AgreementController {

    private final AgreementService agreementService;

    /**
     * Get all active agreements for current authenticated user (as landlord or tenant).
     */
    @GetMapping("/my-agreements")
    @PreAuthorize("hasAnyRole('CITIZEN','LANDLORD','TENANT','ADMIN')")
    public ResponseEntity<List<AgreementResponse>> getMyAgreements(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(agreementService.getMyAgreements(userDetails.getUsername()));
    }

    /**
     * Get agreements where current authenticated user is the landlord.
     */
    @GetMapping("/landlord")
    @PreAuthorize("hasAnyRole('LANDLORD','BOTH','ADMIN','SYSTEM_ADMINISTRATOR')")
    public ResponseEntity<List<AgreementResponse>> getLandlordAgreements(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(agreementService.getLandlordAgreements(userDetails.getUsername()));
    }

    /**
     * Get agreements where current authenticated user is the tenant.
     */
    @GetMapping("/tenant")
    @PreAuthorize("hasAnyRole('TENANT','BOTH','ADMIN','SYSTEM_ADMINISTRATOR')")
    public ResponseEntity<List<AgreementResponse>> getTenantAgreements(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(agreementService.getTenantAgreements(userDetails.getUsername()));
    }

    /**
     * Get active agreements specifically for tenant (TENANT role only).
     * Dual/both accounts use /api/v1/agreements/landlord or /api/v1/agreements/active.
     */
    @GetMapping("/tenant/active")
    @PreAuthorize("hasRole('TENANT')")
    public ResponseEntity<List<AgreementResponse>> getTenantActiveAgreements(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(agreementService.getTenantActiveAgreements(userDetails.getUsername()));
    }

    /**
     * Get agreement by agreement number.
     */
    @GetMapping("/{agreementNumber}")
    @PreAuthorize("hasAnyRole('CITIZEN','LANDLORD','TENANT','WOREDA_OFFICER','WOREDA_SUPERVISOR','GOVERNMENT_EMPLOYEE','ADMIN')")
    public ResponseEntity<AgreementResponse> getAgreementByNumber(@PathVariable String agreementNumber) {
        return ResponseEntity.ok(agreementService.getAgreementByNumber(agreementNumber));
    }

    /**
     * Get agreement by lease request code.
     */
    @GetMapping("/by-request/{requestCode}")
    @PreAuthorize("hasAnyRole('CITIZEN','LANDLORD','TENANT','WOREDA_OFFICER','WOREDA_SUPERVISOR','GOVERNMENT_EMPLOYEE','ADMIN')")
    public ResponseEntity<AgreementResponse> getAgreementByRequestCode(@PathVariable String requestCode) {
        return ResponseEntity.ok(agreementService.getAgreementByRequestCode(requestCode));
    }

    /**
     * Get all active agreements (for officers, supervisors, administrators, auditors).
     */
    @GetMapping({"", "/active"})
    @PreAuthorize("hasAnyRole('WOREDA_OFFICER','WOREDA_SUPERVISOR','SUB_CITY_ADMINISTRATOR','CITY_ADMINISTRATOR','TAX_OFFICER','SYSTEM_ADMINISTRATOR','AUDITOR','ADMIN')")
    public ResponseEntity<List<AgreementResponse>> getAllActiveAgreements() {
        return ResponseEntity.ok(agreementService.getAllActiveAgreements());
    }

    /**
     * Renew agreement for an additional 2 years (24 months).
     */
    @PostMapping("/{agreementNumber}/renew")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<java.util.Map<String, String>> renewAgreement(
            @PathVariable String agreementNumber,
            @AuthenticationPrincipal UserDetails userDetails) {
        agreementService.renewAgreement(agreementNumber, userDetails.getUsername());
        return ResponseEntity.ok(java.util.Map.of(
                "message", "Agreement renewed successfully for an additional 2 years (24 months)."
        ));
    }

    /**
     * Landlord requests cancellation. Starts a 60-day window for tenant to accept.
     */
    @PostMapping("/{agreementNumber}/request-cancellation")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<java.util.Map<String, String>> requestCancellation(
            @PathVariable String agreementNumber,
            @AuthenticationPrincipal UserDetails userDetails) {
        agreementService.requestCancellation(agreementNumber, userDetails.getUsername());
        return ResponseEntity.ok(java.util.Map.of(
                "message", "Cancellation request submitted. Tenant has 60 days to accept, otherwise the system will auto-cancel."
        ));
    }

    /**
     * Tenant accepts cancellation request. Immediately cancels agreement and releases property.
     */
    @PostMapping("/{agreementNumber}/accept-cancellation")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<java.util.Map<String, String>> acceptCancellation(
            @PathVariable String agreementNumber,
            @AuthenticationPrincipal UserDetails userDetails) {
        agreementService.acceptCancellation(agreementNumber, userDetails.getUsername());
        return ResponseEntity.ok(java.util.Map.of(
                "message", "Agreement cancelled successfully. Property is now available for lease."
        ));
    }

    /**
     * Tenant cancels active agreement immediately. Immediately cancels agreement and releases property.
     */
    @PostMapping("/{agreementNumber}/tenant-cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<java.util.Map<String, String>> tenantCancelAgreement(
            @PathVariable String agreementNumber,
            @AuthenticationPrincipal UserDetails userDetails) {
        agreementService.tenantCancelAgreement(agreementNumber, userDetails.getUsername());
        return ResponseEntity.ok(java.util.Map.of(
                "message", "Agreement cancelled successfully by tenant. Property is now available for lease."
        ));
    }
}
