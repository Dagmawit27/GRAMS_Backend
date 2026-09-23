package com.ethiorental.backend.payment.controller;

import com.ethiorental.backend.payment.dto.request.PaymentInitiateRequest;
import com.ethiorental.backend.payment.dto.response.PaymentInitiateResponse;
import com.ethiorental.backend.payment.dto.response.PaymentResponse;
import com.ethiorental.backend.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final com.ethiorental.backend.payment.scheduler.RentReminderScheduler rentReminderScheduler;

    /**
     * Initialize advance rent payment with Chapa
     */
    @PostMapping("/initialize")
    public ResponseEntity<PaymentInitiateResponse> initializePayment(
            @Valid @RequestBody PaymentInitiateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        String username = userDetails != null ? userDetails.getUsername() : "anonymous";
        return ResponseEntity.ok(paymentService.initiateAdvancePayment(username, request));
    }

    /**
     * Verify payment status with Chapa and settle transaction
     */
    @GetMapping("/verify/{txRef}")
    public ResponseEntity<PaymentResponse> verifyPayment(@PathVariable String txRef) {
        return ResponseEntity.ok(paymentService.verifyAndCompletePayment(txRef));
    }

    /**
     * Get payment details by transaction reference
     */
    @GetMapping("/{txRef}")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable String txRef) {
        return ResponseEntity.ok(paymentService.getPaymentByTxRef(txRef));
    }

    /**
     * Get payment records for a specific agreement
     */
    @GetMapping("/agreement/{identifier}")
    public ResponseEntity<List<PaymentResponse>> getAgreementPayments(@PathVariable String identifier) {
        return ResponseEntity.ok(paymentService.getPaymentsForAgreement(identifier));
    }

    /**
     * Get all payment records for current authenticated user (as tenant or landlord)
     */
    @GetMapping("/my-payments")
    public ResponseEntity<List<PaymentResponse>> getMyPayments(@AuthenticationPrincipal UserDetails userDetails) {
        String username = userDetails != null ? userDetails.getUsername() : "";
        return ResponseEntity.ok(paymentService.getMyPayments(username));
    }

    /**
     * Chapa Webhook receiver
     */
    @PostMapping("/webhook")
    public ResponseEntity<Void> handleChapaWebhook(
            @RequestBody(required = false) String payload,
            @RequestHeader(value = "x-chapa-signature", required = false) String signatureHeader) {
        log.info("Chapa Webhook triggered with signature header: {}", signatureHeader != null ? "PRESENT" : "ABSENT");
        paymentService.handleWebhook(payload, signatureHeader);
        return ResponseEntity.ok().build();
    }

    /**
     * Manually trigger rent due date check and reminder dispatch
     */
    @PostMapping("/trigger-reminders")
    public ResponseEntity<Map<String, Object>> triggerReminders() {
        rentReminderScheduler.checkRentDueDates();
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Rent payment due date check executed successfully"
        ));
    }
}
