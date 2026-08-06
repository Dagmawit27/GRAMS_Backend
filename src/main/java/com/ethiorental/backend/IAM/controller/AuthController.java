package com.ethiorental.backend.IAM.controller;

import com.ethiorental.backend.IAM.dto.request.LoginRequest;
import com.ethiorental.backend.IAM.dto.request.RefreshTokenRequest;
import com.ethiorental.backend.IAM.dto.request.RegisterCitizenRequest;
import com.ethiorental.backend.IAM.dto.response.AuthResponse;
import com.ethiorental.backend.IAM.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** Citizen self-registration */
    @PostMapping("/register/citizen")
    public ResponseEntity<AuthResponse> registerCitizen(@Valid @RequestBody RegisterCitizenRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerCitizen(request));
    }

    /** Citizen login (email / phone / faydaId + password) */
    @PostMapping("/login/citizen")
    public ResponseEntity<AuthResponse> loginCitizen(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.loginCitizen(request));
    }

    /** Employee login (email + password) */
    @PostMapping("/login/employee")
    public ResponseEntity<AuthResponse> loginEmployee(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.loginEmployee(request));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            @RequestBody(required = false) RefreshTokenRequest request) {
        if (request != null) authService.logout(request.getRefreshToken());
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }
}
