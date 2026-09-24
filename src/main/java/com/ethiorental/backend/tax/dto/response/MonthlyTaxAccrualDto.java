package com.ethiorental.backend.tax.dto.response;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonthlyTaxAccrualDto {
    private String ethiopianMonth;
    private String gregorianMonth;
    private BigDecimal rentalIncome;
    private BigDecimal accruedTax;
    private Boolean isSummerSettlementMonth;
    private Boolean isSettled;
}
