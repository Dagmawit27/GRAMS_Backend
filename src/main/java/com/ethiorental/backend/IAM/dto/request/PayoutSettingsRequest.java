package com.ethiorental.backend.IAM.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayoutSettingsRequest {

    /** Taxpayer Identification Number (TIN) required by NBE and Ministry of Revenues for rental payouts */
    private String tinNumber;

    /** Primary preferred payment method or bank name (e.g. "Commercial Bank of Ethiopia (CBE)", "Telebirr") */
    @NotBlank(message = "Primary bank or payment method is required")
    private String bankName;

    /** Primary payout bank or mobile money account number */
    @NotBlank(message = "Primary account number is required")
    private String accountNumber;

    /** Primary account holder full legal name */
    private String accountHolderName;

    /** Short payment provider key or primary account selector (e.g. "CBE", "Telebirr", "AWASH") */
    private String preferredPaymentMethod;

    /** Secondary payout bank or payment provider name */
    private String bankName2;

    /** Secondary payout account number */
    private String accountNumber2;

    /** Secondary account holder name */
    private String accountHolderName2;

    /** Tertiary payout bank or payment provider name */
    private String bankName3;

    /** Tertiary payout account number */
    private String accountNumber3;

    /** Tertiary account holder name */
    private String accountHolderName3;
}
