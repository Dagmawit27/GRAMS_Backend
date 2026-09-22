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
    private BigDecimal totalGrossRentalIncome;
    private BigDecimal totalEstimatedAnnualTax;
    private BigDecimal effectiveTaxRate;
    private String filingStatus;
    private String summerFilingDeadline;
    private boolean isSummerWindowOpen;
    private String clearanceCertificateNumber;
    private Instant settledAt;
    private List<AgreementTaxBreakdownDto> agreements;
    private List<MonthlyTaxAccrualDto> monthlyAccruals;
    private String legalProclamationNotice;
}
