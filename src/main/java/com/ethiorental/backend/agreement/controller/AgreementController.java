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
import java.util.Map;

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
    @PreAuthorize("hasAnyRole('CITIZEN','LANDLORD','ADMIN')")
    public ResponseEntity<List<AgreementResponse>> getLandlordAgreements(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(agreementService.getLandlordAgreements(userDetails.getUsername()));
    }

    /**
     * Get agreements where current authenticated user is the tenant.
     */
    @GetMapping("/tenant")
    @PreAuthorize("hasAnyRole('CITIZEN','TENANT','ADMIN')")
    public ResponseEntity<List<AgreementResponse>> getTenantAgreements(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(agreementService.getTenantAgreements(userDetails.getUsername()));
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

    @PostMapping("/{agreementNumber}/renew")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> renewAgreement(
            @PathVariable String agreementNumber,
            @AuthenticationPrincipal UserDetails userDetails) {
        agreementService.renewAgreement(agreementNumber, userDetails.getUsername());
        return ResponseEntity.ok(Map.of("message", "Agreement renewed successfully for an additional 24 months."));
    }

    @PostMapping("/{agreementNumber}/request-cancellation")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> requestCancellation(
            @PathVariable String agreementNumber,
            @AuthenticationPrincipal UserDetails userDetails) {
        agreementService.requestCancellation(agreementNumber, userDetails.getUsername());
        return ResponseEntity.ok(Map.of("message", "Cancellation request submitted. Tenant has 60 days to accept."));
    }

    @PostMapping("/{agreementNumber}/accept-cancellation")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> acceptCancellation(
            @PathVariable String agreementNumber,
            @AuthenticationPrincipal UserDetails userDetails) {
        agreementService.acceptCancellation(agreementNumber, userDetails.getUsername());
        return ResponseEntity.ok(Map.of("message", "Cancellation accepted. Agreement is now cancelled and property is available."));
    }
}
