package com.ethiorental.backend.payment.entity;

import com.ethiorental.backend.IAM.entity.Citizen;
import com.ethiorental.backend.agreement.entity.Agreement;
import com.ethiorental.backend.payment.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(name = "tx_ref", unique = true, nullable = false, updatable = false)
    private String txRef;

    @Column(name = "chapa_reference")
    private String chapaReference;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agreement_id")
    private Agreement agreement;

    @Column(name = "agreement_number")
    private String agreementNumber;

    @Column(name = "request_code")
    private String requestCode;

    @Column(name = "property_title")
    private String propertyTitle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Citizen tenant;

    @Column(name = "tenant_name")
    private String tenantName;

    @Column(name = "tenant_email")
    private String tenantEmail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "landlord_id", nullable = false)
    private Citizen landlord;

    @Column(name = "landlord_name")
    private String landlordName;

    @Column(name = "landlord_email")
    private String landlordEmail;

    @Column(name = "landlord_bank_name")
    private String landlordBankName;

    @Column(name = "landlord_account_number")
    private String landlordAccountNumber;

    @Column(name = "landlord_account_holder_name")
    private String landlordAccountHolderName;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "tax_amount", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "net_landlord_amount", precision = 12, scale = 2)
    private BigDecimal netLandlordAmount;

    @Column(nullable = false, length = 10)
    @Builder.Default
    private String currency = "ETB";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(name = "payment_method")
    private String paymentMethod;

    @Column(name = "checkout_url", length = 1000)
    private String checkoutUrl;

    @Column(name = "payment_date")
    private LocalDateTime paymentDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.netLandlordAmount == null && this.amount != null) {
            this.netLandlordAmount = this.amount.subtract(this.taxAmount != null ? this.taxAmount : BigDecimal.ZERO);
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
