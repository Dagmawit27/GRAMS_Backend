package com.ethiorental.backend.IAM.controller;

import com.ethiorental.backend.IAM.dto.response.UserSummaryDto;
import com.ethiorental.backend.IAM.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/officer")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('WOREDA_OFFICER','WOREDA_SUPERVISOR','SUB_CITY_ADMINISTRATOR'," +
              "'CITY_ADMINISTRATOR','TAX_OFFICER','SYSTEM_ADMINISTRATOR','AUDITOR')")
public class OfficerPortalController {

    private final UserService userService;

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getOfficerDashboard() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserSummaryDto profile = userService.getCurrentUserProfile(auth.getName());
        return ResponseEntity.ok(Map.of(
                "portal", "Government Officer Portal",
                "message", "Welcome to the Official Government Rental Verification Portal",
                "officer", profile
        ));
    }
}
