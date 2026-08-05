package com.ethiorental.backend.IAM.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.ethiorental.backend.IAM.dto.*;
import com.ethiorental.backend.IAM.service.AuthService;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register/citizen")
    public ResponseEntity<AuthResponse> registerCitizen(@Valid @RequestBody RegisterCitizenRequest request) {
        AuthResponse response = authService.registerCitizen(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/register/employee")
    public ResponseEntity<AuthResponse> registerEmployee(@Valid @RequestBody RegisterEmployeeRequest request) {
        AuthResponse response = authService.registerEmployee(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login/citizen")
    public ResponseEntity<AuthResponse> loginCitizen(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.loginCitizen(request);
        return ResponseEntity.ok(response);
    }


    // @PostMapping("/login/employee")
    // public ResponseEntity<AuthResponse> loginEmployee(@Valid @RequestBody LoginRequest request) {
    //     AuthResponse response = authService.loginEmployee(request);
    //     return ResponseEntity.ok(response);
    // }

    @PostMapping("/refresh-token")
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(@RequestBody(required = false) RefreshTokenRequest request) {
        if (request != null) {
            authService.logout(request.getRefreshToken());
        }
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }
}
