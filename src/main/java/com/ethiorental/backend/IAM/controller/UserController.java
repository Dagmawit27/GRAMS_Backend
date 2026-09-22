package com.ethiorental.backend.IAM.controller;

import com.ethiorental.backend.IAM.dto.request.ChangePasswordRequest;
import com.ethiorental.backend.IAM.dto.request.PayoutSettingsRequest;
import com.ethiorental.backend.IAM.dto.request.UpdateProfileRequest;
import com.ethiorental.backend.IAM.dto.response.UserSummaryDto;
import com.ethiorental.backend.IAM.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /** Returns the profile of the currently authenticated user (citizen or employee) */
    @GetMapping("/me")
    public ResponseEntity<UserSummaryDto> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return ResponseEntity.ok(userService.getCurrentUserProfile(auth.getName()));
    }

    /** Update personal information and coordinates for the authenticated citizen */
    @PutMapping("/profile")
    public ResponseEntity<UserSummaryDto> updateProfile(
            @RequestBody UpdateProfileRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(userService.updateCitizenProfile(userDetails.getUsername(), request));
    }

    /** Update payout and bank account settings for the authenticated citizen (landlord) */
    @PutMapping("/payout-settings")
    public ResponseEntity<UserSummaryDto> updatePayoutSettings(
            @Valid @RequestBody PayoutSettingsRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(userService.updatePayoutSettings(userDetails.getUsername(), request));
    }

    /** Change password for the authenticated user */
    @PutMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        userService.changePassword(userDetails.getUsername(), request);
        return ResponseEntity.ok(Map.of("message", "Password changed successfully."));
    }
}
