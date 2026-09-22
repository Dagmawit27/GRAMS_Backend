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
public class PaymentInitiateResponse {

    private String txRef;
    private String checkoutUrl;
    private BigDecimal amount;
    private String currency;
    private String status;
    private String requestCode;
    private String agreementNumber;
    private String propertyTitle;
    private String landlordName;
    private String landlordBankName;
    private String landlordAccountNumber;
    private String landlordAccountHolderName;
    private boolean sandboxMode;
}
