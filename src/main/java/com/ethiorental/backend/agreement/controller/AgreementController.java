package com.ethiorental.backend.agreement.controller;

import com.ethiorental.backend.agreement.dto.AgreementRequest;
import com.ethiorental.backend.agreement.dto.AgreementResponse;
import com.ethiorental.backend.agreement.service.AgreementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/agreements")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AgreementController {

    private final AgreementService agreementService;

    /**
     * Generate agreement for an approved lease request - landlord only.
     */
    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('LANDLORD','CITIZEN','BOTH')")
    public ResponseEntity<AgreementResponse> generateAgreement(
            @RequestBody AgreementRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        return ResponseEntity.ok(agreementService.generateAgreement(request.getRequestCode(), userDetails.getUsername()));
    }

    /**
     * Sign agreement by landlord - landlord only.
     */
    @PostMapping("/sign")
    @PreAuthorize("hasAnyRole('LANDLORD','CITIZEN','BOTH')")
    public ResponseEntity<AgreementResponse> signAgreement(
            @RequestBody AgreementRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        return ResponseEntity.ok(agreementService.signAgreement(request.getRequestCode(), request.getOtp(), userDetails.getUsername()));
    }

    /**
     * Sign agreement by tenant - tenant only.
     */
    @PostMapping("/sign-tenant")
    @PreAuthorize("hasAnyRole('TENANT','CITIZEN','BOTH')")
    public ResponseEntity<AgreementResponse> signAgreementByTenant(
            @RequestBody AgreementRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        return ResponseEntity.ok(agreementService.signAgreementByTenant(request.getRequestCode(), request.getOtp(), userDetails.getUsername()));
    }

    /**
     * Get agreement by request code.
     */
    @GetMapping("/request/{requestCode}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AgreementResponse> getAgreementByRequestCode(
            @PathVariable String requestCode,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        return ResponseEntity.ok(agreementService.getAgreementByRequestCode(requestCode));
    }

    /**
     * Get agreement by agreement code.
     */
    @GetMapping("/{agreementCode}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AgreementResponse> getAgreementByAgreementCode(
            @PathVariable String agreementCode,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        return ResponseEntity.ok(agreementService.getAgreementByAgreementCode(agreementCode));
    }
}
