package com.ethiorental.backend.IAM.controller;

import com.ethiorental.backend.IAM.dto.request.RegisterEmployeeRequest;
import com.ethiorental.backend.IAM.dto.response.AuthResponse;
import com.ethiorental.backend.IAM.dto.response.UserSummaryDto;
import com.ethiorental.backend.IAM.service.AuthService;
import com.ethiorental.backend.IAM.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'ROLE_SYSTEM_ADMINISTRATOR')")
public class AdminEmployeeController {

    private final AuthService authService;
    private final UserService userService;

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getAdminDashboard() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        UserSummaryDto adminProfile = userService.getUserProfileByEmail(email);

        return ResponseEntity.ok(Map.of(
                "portal", "System Administrator Portal",
                "message", "Welcome to the GRAMS System Administration Portal",
                "admin", adminProfile
        ));
    }

    /**
     * Government employee registration endpoint.
     * Restricted to System Administrators only for security.
     */
    @PostMapping("/employees/register")
    public ResponseEntity<AuthResponse> registerEmployee(@Valid @RequestBody RegisterEmployeeRequest request) {
        AuthResponse response = authService.registerEmployee(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
