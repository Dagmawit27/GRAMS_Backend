package com.ethiorental.backend.IAM.service;

import com.ethiorental.backend.IAM.dto.request.ChangePasswordRequest;
import com.ethiorental.backend.IAM.dto.request.PayoutSettingsRequest;
import com.ethiorental.backend.IAM.dto.request.UpdateProfileRequest;
import com.ethiorental.backend.IAM.dto.response.UserSummaryDto;
import com.ethiorental.backend.IAM.entity.Citizen;
import com.ethiorental.backend.IAM.repository.CitizenCredentialRepository;
import com.ethiorental.backend.IAM.repository.CitizenRepository;
import com.ethiorental.backend.IAM.repository.EmployeeCredentialRepository;
import com.ethiorental.backend.agreement.entity.Agreement;
import com.ethiorental.backend.agreement.repository.AgreementRepository;
import com.ethiorental.backend.lease.entity.LeaseRequest;
import com.ethiorental.backend.lease.repository.LeaseRequestRepository;
import com.ethiorental.backend.notification.controller.NotificationSseController;
import com.ethiorental.backend.notification.dto.response.NotificationResponse;
import com.ethiorental.backend.notification.entity.Notification;
import com.ethiorental.backend.notification.repository.NotificationRepository;
import com.ethiorental.backend.notification.service.NotificationService;
import com.ethiorental.backend.shared.notification.NotificationChannel;
import com.ethiorental.backend.shared.notification.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final AuthService authService;
    private final CitizenRepository citizenRepository;
    private final CitizenCredentialRepository citizenCredentialRepository;
    private final EmployeeCredentialRepository employeeCredentialRepository;
    private final PasswordEncoder passwordEncoder;
    private final AgreementRepository agreementRepository;
    private final LeaseRequestRepository leaseRequestRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;
    private final NotificationSseController sseController;

    public UserSummaryDto getCurrentUserProfile(String username) {
        return authService.buildUserSummary(username);
    }

    @Transactional
    public UserSummaryDto updatePayoutSettings(String username, PayoutSettingsRequest request) {
        Citizen citizen = citizenRepository.findByEmail(username)
                .orElseThrow(() -> new IllegalArgumentException("Citizen profile not found for: " + username));

        // Taxpayer Identification Number (TIN)
        if (request.getTinNumber() != null) {
            String trimmed = request.getTinNumber().trim();
            citizen.setTinNumber(trimmed.isEmpty() ? null : trimmed);
        }

        // Account 1 (Primary)
        citizen.setBankName(request.getBankName());
        citizen.setAccountNumber(request.getAccountNumber());
        citizen.setAccountHolderName(request.getAccountHolderName());
        citizen.setPreferredPaymentMethod(request.getPreferredPaymentMethod() != null
                ? request.getPreferredPaymentMethod()
                : request.getBankName());

        // Account 2 (Secondary)
        citizen.setBankName2(request.getBankName2());
        citizen.setAccountNumber2(request.getAccountNumber2());
        citizen.setAccountHolderName2(request.getAccountHolderName2());

        // Account 3 (Tertiary)
        citizen.setBankName3(request.getBankName3());
        citizen.setAccountNumber3(request.getAccountNumber3());
        citizen.setAccountHolderName3(request.getAccountHolderName3());

        // Save & Flush to ensure immediate persistence
        citizen = citizenRepository.saveAndFlush(citizen);

        // Build summary DTO
        UserSummaryDto summary = authService.buildUserSummary(username);

        // Broadcast real-time SSE notifications to tenants in a safe block so notification errors do not abort the update
        try {
            notifyTenantsOfPayoutUpdate(username, citizen);
        } catch (Exception ex) {
            log.warn("Could not dispatch SSE payout update for {}: {}", username, ex.getMessage());
        }

        return summary;
    }

    @Transactional
    public UserSummaryDto updateCitizenProfile(String username, UpdateProfileRequest request) {
        Citizen citizen = citizenRepository.findByEmail(username)
                .orElseThrow(() -> new IllegalArgumentException("Citizen profile not found for: " + username));

        if (request.getFirstName() != null && !request.getFirstName().isBlank()) {
            citizen.setFirstName(request.getFirstName().trim());
        }
        if (request.getMiddleName() != null) {
            citizen.setMiddleName(request.getMiddleName().trim());
        }
        if (request.getLastName() != null && !request.getLastName().isBlank()) {
            citizen.setLastName(request.getLastName().trim());
        }
        if (request.getPhone() != null && !request.getPhone().isBlank()) {
            citizen.setPhone(request.getPhone().trim());
        }
        if (request.getCity() != null) {
            citizen.setCity(request.getCity().trim());
        }
        if (request.getSubCity() != null) {
            citizen.setSubCity(request.getSubCity().trim());
        }
        if (request.getWoreda() != null) {
            citizen.setWoreda(request.getWoreda().trim());
        }
        if (request.getHouseNumber() != null) {
            citizen.setHouseNumber(request.getHouseNumber().trim());
        }
        if (request.getWorksOn() != null) {
            citizen.setWorksOn(request.getWorksOn().trim());
        }
        if (request.getTinNumber() != null) {
            citizen.setTinNumber(request.getTinNumber().trim());
        }
        if (request.getEmergencyContactName() != null) {
            citizen.setEmergencyContactName(request.getEmergencyContactName().trim());
        }
        if (request.getEmergencyContactPhone() != null) {
            citizen.setEmergencyContactPhone(request.getEmergencyContactPhone().trim());
        }

        citizenRepository.save(citizen);
        return authService.buildUserSummary(username);
    }

    @Transactional
    public void changePassword(String username, ChangePasswordRequest request) {
        if (request.getNewPassword() == null || request.getNewPassword().length() < 6) {
            throw new IllegalArgumentException("New password must be at least 6 characters long.");
        }
        if (request.getConfirmPassword() != null && !request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("New passwords do not match.");
        }

        var citizenCredOpt = citizenCredentialRepository.findByEmail(username);
        if (citizenCredOpt.isPresent()) {
            var cred = citizenCredOpt.get();
            if (!passwordEncoder.matches(request.getCurrentPassword(), cred.getPasswordHash())) {
                throw new IllegalArgumentException("Current password is incorrect.");
            }
            cred.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
            citizenCredentialRepository.save(cred);
            return;
        }

        var empCredOpt = employeeCredentialRepository.findByEmail(username);
        if (empCredOpt.isPresent()) {
            var cred = empCredOpt.get();
            if (!passwordEncoder.matches(request.getCurrentPassword(), cred.getPasswordHash())) {
                throw new IllegalArgumentException("Current password is incorrect.");
            }
            cred.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
            employeeCredentialRepository.save(cred);
            return;
        }

        throw new IllegalArgumentException("User credential record not found for: " + username);
    }

    private void notifyTenantsOfPayoutUpdate(String landlordEmail, Citizen landlord) {
        try {
            Set<String> notifiedEmails = new HashSet<>();

            // 1. Notify tenants on all agreements with this landlord
            List<Agreement> agreements = agreementRepository.findByLandlordEmail(landlordEmail);
            for (Agreement a : agreements) {
                if (a.getTenant() != null && a.getTenant().getEmail() != null) {
                    String tEmail = a.getTenant().getEmail();
                    if (notifiedEmails.add(tEmail)) {
                        sendPayoutEvent(tEmail, landlord, a.getRequestCode() != null ? a.getRequestCode() : a.getAgreementNumber());
                    }
                }
            }

            // 2. Notify tenants on any active lease requests with this landlord
            List<LeaseRequest> leaseRequests = leaseRequestRepository.findByLandlordId(landlord.getId());
            for (LeaseRequest lr : leaseRequests) {
                if (lr.getApplicant() != null && lr.getApplicant().getEmail() != null) {
                    String tEmail = lr.getApplicant().getEmail();
                    if (notifiedEmails.add(tEmail)) {
                        sendPayoutEvent(tEmail, landlord, lr.getRequestCode());
                    }
                }
            }

            // 3. Also notify landlord's own channel for multi-device/tab confirmation
            sendPayoutEvent(landlordEmail, landlord, "PROFILE");

            log.info("Notified {} tenants via SSE of landlord {} payout settings update", notifiedEmails.size(), landlordEmail);
        } catch (Exception ex) {
            log.error("Failed to notify tenants of landlord payout update for {}: {}", landlordEmail, ex.getMessage(), ex);
        }
    }

    private void sendPayoutEvent(String recipientEmail, Citizen landlord, String refCode) {
        try {
            String landlordFullName = (landlord.getFirstName() != null ? landlord.getFirstName() : "") +
                    (landlord.getLastName() != null ? " " + landlord.getLastName() : "");

            Notification notification = new Notification();
            notification.setRecipientUserId(recipientEmail);
            notification.setType(NotificationType.LANDLORD_PAYOUT_UPDATED);
            notification.setModule("PAYMENT");
            notification.setEntityId(refCode != null ? refCode : landlord.getId().toString());
            notification.setMessage(String.format(
                    "Landlord %s has configured payout details (%s: %s). Advance rent payment is now enabled.",
                    landlordFullName.trim(),
                    landlord.getBankName(),
                    landlord.getAccountNumber()
            ));
            notification.setChannel(NotificationChannel.IN_APP);
            notification.setRead(false);
            notification.setCreatedAt(Instant.now());

            notificationRepository.save(notification);

            NotificationResponse response = notificationService.toNotificationResponse(notification);
            sseController.sendNotificationToUser(recipientEmail, response);

            long unreadCount = notificationService.getUnreadCount(recipientEmail);
            sseController.sendUnreadCountUpdate(recipientEmail, unreadCount);
        } catch (Exception e) {
            log.warn("Could not dispatch SSE payout update to {}: {}", recipientEmail, e.getMessage());
        }
    }
}
