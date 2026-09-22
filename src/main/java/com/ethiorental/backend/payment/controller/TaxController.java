package com.ethiorental.backend.payment.controller;

import com.ethiorental.backend.payment.dto.request.TaxSettlementRequest;
import com.ethiorental.backend.payment.dto.response.TaxSettlementResponse;
import com.ethiorental.backend.payment.dto.response.TaxSummaryResponse;
import com.ethiorental.backend.payment.service.TaxService;
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

    @GetMapping("/summary")
    public ResponseEntity<TaxSummaryResponse> getTaxSummary(@AuthenticationPrincipal UserDetails userDetails) {
        String email = getUserId(userDetails);
        TaxSummaryResponse response = taxService.getLandlordTaxSummary(email);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/settle")
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

    private String getUserId(UserDetails userDetails) {
        if (userDetails == null || userDetails.getUsername() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not authenticated");
        }
        return userDetails.getUsername();
    }
}
