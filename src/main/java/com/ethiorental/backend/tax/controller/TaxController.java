package com.ethiorental.backend.tax.controller;

import com.ethiorental.backend.tax.dto.request.TaxSettlementRequest;
import com.ethiorental.backend.tax.dto.response.TaxSettlementResponse;
import com.ethiorental.backend.tax.dto.response.TaxSummaryResponse;
import com.ethiorental.backend.tax.entity.LandlordTax;
import com.ethiorental.backend.tax.repository.LandlordTaxRepository;
import com.ethiorental.backend.tax.service.TaxService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/tax")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class TaxController {

    private final TaxService taxService;
    private final LandlordTaxRepository landlordTaxRepository;

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('LANDLORD','BOTH')")
    public ResponseEntity<TaxSummaryResponse> getTaxSummary(@AuthenticationPrincipal UserDetails userDetails) {
        String email = getUserId(userDetails);
        TaxSummaryResponse response = taxService.getLandlordTaxSummary(email);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/settle")
    @PreAuthorize("hasAnyRole('LANDLORD','BOTH')")
    public ResponseEntity<TaxSettlementResponse> settleAnnualTax(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody TaxSettlementRequest request
    ) {
        String email = getUserId(userDetails);
        TaxSettlementResponse response = taxService.settleAnnualTax(email, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/proclamation")
    public ResponseEntity<Map<String, String>> getLegalProclamation() {
        return ResponseEntity.ok(Map.of("notice", TaxService.LEGAL_PROCLAMATION_NOTICE));
    }

    @GetMapping("/ledger")
    @PreAuthorize("hasAnyRole('LANDLORD','BOTH')")
    public ResponseEntity<LandlordTax> getTaxLedger(@AuthenticationPrincipal UserDetails userDetails) {
        String email = getUserId(userDetails);
        return landlordTaxRepository.findByLandlordEmailAndFiscalYear(email, TaxService.CURRENT_FISCAL_YEAR)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    private String getUserId(UserDetails userDetails) {
        if (userDetails == null || userDetails.getUsername() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not authenticated");
        }
        return userDetails.getUsername();
    }
}
