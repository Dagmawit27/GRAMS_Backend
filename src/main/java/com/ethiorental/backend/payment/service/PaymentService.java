package com.ethiorental.backend.payment.service;

import com.ethiorental.backend.payment.dto.request.PaymentInitiateRequest;
import com.ethiorental.backend.payment.dto.response.PaymentInitiateResponse;
import com.ethiorental.backend.payment.dto.response.PaymentResponse;

import java.util.List;
import java.util.UUID;

public interface PaymentService {

    PaymentInitiateResponse initiateAdvancePayment(String username, PaymentInitiateRequest request);

    PaymentResponse verifyAndCompletePayment(String txRef);

    PaymentResponse getPaymentByTxRef(String txRef);

    List<PaymentResponse> getPaymentsForAgreement(UUID agreementId);

    List<PaymentResponse> getPaymentsForAgreement(String identifier);

    List<PaymentResponse> getMyPayments(String username);

    void handleWebhook(String payload, String signatureHeader);
}
