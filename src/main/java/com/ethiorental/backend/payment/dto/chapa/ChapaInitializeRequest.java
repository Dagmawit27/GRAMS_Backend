package com.ethiorental.backend.payment.dto.chapa;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChapaInitializeRequest {

    private String amount;

    private String currency;

    private String email;

    @JsonProperty("first_name")
    private String firstName;

    @JsonProperty("last_name")
    private String lastName;

    @JsonProperty("phone_number")
    private String phoneNumber;

    @JsonProperty("tx_ref")
    private String txRef;

    @JsonProperty("callback_url")
    private String callbackUrl;

    @JsonProperty("return_url")
    private String returnUrl;

    private Map<String, Object> customization;

    private Map<String, Object> meta;
}
