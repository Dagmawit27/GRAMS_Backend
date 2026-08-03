package com.ethiorental.backend.IAM.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "Login identifier (email, phone number, or Fayda ID) is required")
    private String loginIdentifier;

    @NotBlank(message = "Password is required")
    private String password;
}
