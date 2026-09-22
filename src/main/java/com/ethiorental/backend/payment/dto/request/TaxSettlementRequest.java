package com.ethiorental.backend.payment.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaxSettlementRequest {
    private String fiscalYear;

    @NotNull(message = "Settlement amount is required")
    private BigDecimal amount;

    private String paymentMethod;
    private String payerPhoneNumber;
    private String taxpayerTin;
}
