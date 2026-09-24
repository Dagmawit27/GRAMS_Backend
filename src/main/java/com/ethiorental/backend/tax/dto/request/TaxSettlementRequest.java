package com.ethiorental.backend.tax.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaxSettlementRequest {

    @NotNull(message = "Tax amount is required")
    @DecimalMin(value = "0.01", message = "Tax amount must be greater than zero")
    private BigDecimal amount;

    private String fiscalYear;
    private String taxpayerTin;
    private String paymentMethod;
}
