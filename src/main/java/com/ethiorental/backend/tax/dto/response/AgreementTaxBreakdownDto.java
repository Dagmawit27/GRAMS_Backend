package com.ethiorental.backend.tax.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgreementTaxBreakdownDto {
    private UUID agreementId;
    private String agreementNumber;
    private String requestCode;
    private String propertyCode;
    private String propertyTitle;
    private String tenantName;
    private String tenantTin;
    private BigDecimal monthlyRent;
    private Integer monthsCounted;
    private BigDecimal grossIncome;
    private BigDecimal accruedTaxContribution;
    private String status;
}
