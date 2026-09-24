package com.ethiorental.backend.tax.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * TaxRule entity for configurable tax rules per Ethiopian Federal Income Tax Proclamation.
 * Configured per Proclamation No. 979/2016 and Amendment Proclamation No. 1395/2017 E.C.
 */
@Entity
@Table(name = "tax_rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaxRule {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(name = "proclamation_number", nullable = false)
    @Builder.Default
    private String proclamationNumber = "1395/2017";

    @Column(name = "proclamation_year", nullable = false)
    @Builder.Default
    private Integer proclamationYear = 2017;

    /**
     * Statutory deduction percentage for individuals without books (50% per Article 15(5)(b)).
     */
    @Column(name = "deduction_percentage", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal deductionPercentage = BigDecimal.valueOf(50.00);

    @Column(name = "tax_free_threshold", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal taxFreeThreshold = BigDecimal.valueOf(24000.00);

    @Column(name = "bracket_1_rate", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal bracket1Rate = BigDecimal.valueOf(15.00);

    @Column(name = "bracket_1_min", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal bracket1Min = BigDecimal.valueOf(24001.00);

    @Column(name = "bracket_1_max", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal bracket1Max = BigDecimal.valueOf(48000.00);

    @Column(name = "bracket_2_rate", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal bracket2Rate = BigDecimal.valueOf(20.00);

    @Column(name = "bracket_2_min", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal bracket2Min = BigDecimal.valueOf(48001.00);

    @Column(name = "bracket_2_max", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal bracket2Max = BigDecimal.valueOf(84000.00);

    @Column(name = "bracket_3_rate", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal bracket3Rate = BigDecimal.valueOf(25.00);

    @Column(name = "bracket_3_min", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal bracket3Min = BigDecimal.valueOf(84001.00);

    @Column(name = "bracket_3_max", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal bracket3Max = BigDecimal.valueOf(120000.00);

    @Column(name = "bracket_4_rate", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal bracket4Rate = BigDecimal.valueOf(30.00);

    @Column(name = "bracket_4_min", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal bracket4Min = BigDecimal.valueOf(120001.00);

    @Column(name = "bracket_4_max", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal bracket4Max = BigDecimal.valueOf(168000.00);

    @Column(name = "bracket_5_rate", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal bracket5Rate = BigDecimal.valueOf(35.00);

    @Column(name = "bracket_5_min", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal bracket5Min = BigDecimal.valueOf(168001.00);

    /**
     * Corporate flat tax rate for bodies (Article 14 - 30%).
     */
    @Column(name = "corporate_tax_rate", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal corporateTaxRate = BigDecimal.valueOf(30.00);

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "created_by", nullable = false, length = 255)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_by", length = 255)
    private String updatedBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.effectiveFrom == null) {
            this.effectiveFrom = LocalDate.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
