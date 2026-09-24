package com.ethiorental.backend.tax.entity;

import com.ethiorental.backend.IAM.entity.Citizen;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * LandlordTax tracks Schedule B rental tax liability per landlord per Ethiopian fiscal year.
 * Synchronized automatically when new active agreements are registered or rent payments settle.
 */
@Entity
@Table(name = "landlord_taxes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LandlordTax {

    @Id
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "landlord_id", nullable = false)
    private Citizen landlord;

    @Column(name = "landlord_email", nullable = false, length = 255)
    private String landlordEmail;

    @Column(name = "fiscal_year", nullable = false, length = 50)
    private String fiscalYear;

    /**
     * Total number of currently active lease agreements for this landlord.
     */
    @Column(name = "total_agreements", nullable = false)
    @Builder.Default
    private Integer totalAgreements = 0;

    /**
     * Sum of monthly rents across ALL active agreements for this landlord.
     */
    @Column(name = "total_contracted_monthly_rent", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalContractedMonthlyRent = BigDecimal.ZERO;

    /**
     * Projected annual gross income from all active agreements (totalContractedMonthlyRent * 12).
     */
    @Column(name = "projected_annual_gross_income", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal projectedAnnualGrossIncome = BigDecimal.ZERO;

    /**
     * Cumulative actual gross rental revenue collected via completed payments.
     */
    @Column(name = "total_gross_income", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalGrossIncome = BigDecimal.ZERO;

    /**
     * Accrued annual Schedule B tax liability calculated according to Ethiopian tax brackets.
     */
    @Column(name = "total_tax_accrued", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalTaxAccrued = BigDecimal.ZERO;

    /**
     * Total tax remitted/paid during summer settlement window.
     */
    @Column(name = "total_tax_paid", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalTaxPaid = BigDecimal.ZERO;

    /**
     * Total rental months covered by completed payments.
     */
    @Column(name = "total_months_paid", nullable = false)
    @Builder.Default
    private Integer totalMonthsPaid = 0;

    /**
     * Filing status: ACCRUING, SUMMER_WINDOW_OPEN, SETTLED_CLEARED
     */
    @Column(name = "tax_status", nullable = false, length = 30)
    @Builder.Default
    private String taxStatus = "ACCRUING";

    @Column(name = "settlement_date")
    private LocalDateTime settlementDate;

    @Column(name = "clearance_certificate_number", length = 100)
    private String clearanceCertificateNumber;

    @Column(name = "last_payment_date")
    private LocalDateTime lastPaymentDate;

    @Column(name = "tax_bracket_percentage")
    private Integer taxBracketPercentage;

    @Column(name = "effective_tax_rate", precision = 5, scale = 2)
    private BigDecimal effectiveTaxRate;

    @Column(name = "net_income_after_tax", precision = 15, scale = 2)
    private BigDecimal netIncomeAfterTax;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.fiscalYear == null) {
            this.fiscalYear = "EFY 2018 (2025/2026 G.C.)";
        }
        recalculateDerivedFields();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        recalculateDerivedFields();
    }

    public void recalculateDerivedFields() {
        if (this.totalGrossIncome != null && this.totalTaxAccrued != null) {
            this.netIncomeAfterTax = this.totalGrossIncome.subtract(this.totalTaxAccrued);
        } else if (this.totalGrossIncome != null) {
            this.netIncomeAfterTax = this.totalGrossIncome;
        }

        if (this.totalGrossIncome != null && this.totalGrossIncome.compareTo(BigDecimal.ZERO) > 0 && this.totalTaxAccrued != null) {
            this.effectiveTaxRate = this.totalTaxAccrued.multiply(BigDecimal.valueOf(100))
                    .divide(this.totalGrossIncome, 2, RoundingMode.HALF_UP);
        } else {
            this.effectiveTaxRate = BigDecimal.ZERO;
        }

        if (this.totalContractedMonthlyRent != null) {
            this.projectedAnnualGrossIncome = this.totalContractedMonthlyRent.multiply(BigDecimal.valueOf(12));
        }
    }
}
