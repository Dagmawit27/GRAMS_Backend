package com.ethiorental.backend.payment.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgreementTaxBreakdownDto {
    private UUID agreementId;
    private String agreementNumber;
    private String requestCode;
    private String propertyCode;
    private String propertyTitle;
    private String tenantName;
    private String tenantTin;
    private BigDecimal monthlyRent;
    private int monthsCounted;
    private BigDecimal grossIncome;
    private BigDecimal accruedTaxContribution;
    private String status;
}
