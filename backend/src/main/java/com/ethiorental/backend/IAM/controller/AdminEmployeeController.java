package com.ethiorental.backend.IAM.controller;

import com.ethiorental.backend.IAM.dto.request.RegisterEmployeeRequest;
import com.ethiorental.backend.IAM.dto.response.AuthResponse;
import com.ethiorental.backend.IAM.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/employees")
@RequiredArgsConstructor
public class AdminEmployeeController {

    private final AuthService authService;

    /**
     * Government employee registration endpoint.
     * Restricted to System Administrators only for security.
     */
    @PostMapping("/register")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'ROLE_SYSTEM_ADMINISTRATOR')")
    public ResponseEntity<AuthResponse> registerEmployee(@Valid @RequestBody RegisterEmployeeRequest request) {
        AuthResponse response = authService.registerEmployee(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
