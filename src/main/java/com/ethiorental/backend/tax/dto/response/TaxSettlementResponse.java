package com.ethiorental.backend.tax.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
