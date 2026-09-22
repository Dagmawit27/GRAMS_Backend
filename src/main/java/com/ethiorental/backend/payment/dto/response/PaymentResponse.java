package com.ethiorental.backend.payment.dto.response;

import com.ethiorental.backend.payment.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {

    private UUID id;
    private String txRef;
    private String chapaReference;
    private UUID agreementId;
    private String agreementNumber;
    private String requestCode;
    private String propertyTitle;

    private String tenantName;
    private String tenantEmail;

    private String landlordName;
    private String landlordEmail;
    private String landlordBankName;
    private String landlordAccountNumber;
    private String landlordAccountHolderName;

    private BigDecimal amount;
    private BigDecimal taxAmount;
    private BigDecimal netLandlordAmount;
    private String currency;
    private PaymentStatus status;
    private String paymentMethod;
    private String checkoutUrl;
    private LocalDateTime paymentDate;
    private LocalDateTime createdAt;
}
