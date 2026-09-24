package com.ethiorental.backend.tax.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaxSummaryResponse {
    private String fiscalYear;
    private String taxpayerName;
    private String tinNumber;
    private BigDecimal totalGrossRentalIncome;
    private BigDecimal totalEstimatedAnnualTax;
    private BigDecimal effectiveTaxRate;
    private BigDecimal netIncomeAfterTax;
    private Integer taxBracketPercentage;
    private Integer totalMonthsPaid;
    private Integer totalAgreementsCount;
    private BigDecimal totalContractedMonthlyRent;
    private BigDecimal projectedAnnualGrossIncome;
    private String filingStatus;
    private String summerFilingDeadline;
    private Boolean isSummerWindowOpen;
    private String clearanceCertificateNumber;
    private Instant settledAt;
    private List<AgreementTaxBreakdownDto> agreements;
    private List<MonthlyTaxAccrualDto> monthlyAccruals;
    private String legalProclamationNotice;
}
