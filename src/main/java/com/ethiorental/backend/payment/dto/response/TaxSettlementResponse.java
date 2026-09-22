package com.ethiorental.backend.payment.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaxSettlementResponse {
    private String status;
    private String clearanceCertificateNumber;
    private BigDecimal amountPaid;
    private String fiscalYear;
    private String taxpayerName;
    private String taxpayerTin;
    private String paymentMethod;
    private Instant settledAt;
    private String receiptPdfUrl;
    private String message;
}
