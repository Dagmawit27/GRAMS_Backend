package com.ethiorental.backend.agreement.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AgreementRequest {
    private String requestCode;
    private String otp;
}
