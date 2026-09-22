package com.ethiorental.backend.payment.dto.chapa;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChapaVerifyResponse {

    private String message;
    private String status;
    private Data data;

    @lombok.Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Data {
        @JsonProperty("first_name")
        private String firstName;

        @JsonProperty("last_name")
        private String lastName;

        private String email;
        private String currency;
        private BigDecimal amount;
        private BigDecimal charge;
        private String mode;
        private String method;
        private String status;
        private String reference;

        @JsonProperty("tx_ref")
        private String txRef;

        private Map<String, Object> meta;
    }
}
