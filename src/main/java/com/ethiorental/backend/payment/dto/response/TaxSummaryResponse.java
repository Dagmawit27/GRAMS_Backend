package com.ethiorental.backend.payment.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaxSummaryResponse {
    private String fiscalYear;
    private String taxpayerName;
    private String tinNumber;
    private BigDecimal totalGrossRentalIncome;          // Total gains from all active agreements
    private BigDecimal totalEstimatedAnnualTax;          // Total tax amount based on Schedule B
    private BigDecimal effectiveTaxRate;                // Tax percentage (e.g., 15.0 for 15%)
    private BigDecimal netIncomeAfterTax;               // Gains minus tax amount
    private Integer taxBracketPercentage;               // Explicit bracket rate (0, 15, 20, 25, 30, 35)
    private Integer totalMonthsPaid;                    // Total months with actual rent payments
    private String filingStatus;
    private String summerFilingDeadline;
    private boolean isSummerWindowOpen;
    private String clearanceCertificateNumber;
    private Instant settledAt;
    private List<AgreementTaxBreakdownDto> agreements;
    private List<MonthlyTaxAccrualDto> monthlyAccruals;
    private String legalProclamationNotice;
}
