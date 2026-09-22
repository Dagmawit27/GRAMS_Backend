package com.ethiorental.backend.payment.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyTaxAccrualDto {
    private String ethiopianMonth;
    private String gregorianMonth;
    private BigDecimal rentalIncome;
    private BigDecimal accruedTax;
    private boolean isSummerSettlementMonth;
    private boolean isSettled;
}
