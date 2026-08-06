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
@PreAuthorize("hasRole('SYSTEM_ADMINISTRATOR')")
public class AdminEmployeeController {

    private final AuthService authService;
    private final UserService userService;

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getAdminDashboard() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserSummaryDto profile = userService.getCurrentUserProfile(auth.getName());
        return ResponseEntity.ok(Map.of(
                "portal", "System Administrator Portal",
                "message", "Welcome to the GRAMS System Administration Portal",
                "admin", profile
        ));
    }

    /**
     * Register a new government employee.
     * Only SYSTEM_ADMINISTRATOR can call this.
     * If no password is provided, default is "Change@{employeeNumber}".
     */
    @PostMapping("/employees/register")
    public ResponseEntity<AuthResponse> registerEmployee(
            @Valid @RequestBody RegisterEmployeeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerEmployee(request));
    }
}
