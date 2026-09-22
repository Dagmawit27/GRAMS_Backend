package com.ethiorental.backend.payment.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentInitiateRequest {

    @NotBlank(message = "Request code or agreement identifier is required")
    private String requestCode;

    private BigDecimal amount;

    private String phoneNumber;

    private String paymentMethod;

    /** Destination landlord bank name chosen by tenant */
    private String destinationBankName;

    /** Destination landlord account number chosen by tenant */
    private String destinationAccountNumber;

    /** Destination landlord account holder name chosen by tenant */
    private String destinationAccountHolderName;
}
