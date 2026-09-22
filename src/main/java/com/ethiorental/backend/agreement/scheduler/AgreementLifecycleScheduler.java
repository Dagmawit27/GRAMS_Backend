package com.ethiorental.backend.agreement.scheduler;

import com.ethiorental.backend.agreement.entity.Agreement;
import com.ethiorental.backend.agreement.enums.AgreementStatus;
import com.ethiorental.backend.agreement.repository.AgreementRepository;
import com.ethiorental.backend.agreement.service.AgreementService;
import com.ethiorental.backend.notification.controller.NotificationSseController;
import com.ethiorental.backend.notification.dto.response.NotificationResponse;
import com.ethiorental.backend.notification.entity.Notification;
import com.ethiorental.backend.notification.repository.NotificationRepository;
import com.ethiorental.backend.notification.service.NotificationService;
import com.ethiorental.backend.shared.notification.NotificationChannel;
import com.ethiorental.backend.shared.notification.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class AgreementLifecycleScheduler {

    private final AgreementRepository agreementRepository;
    private final AgreementService agreementService;
    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;
    private final NotificationSseController sseController;

    /**
     * Daily at 09:00 AM — checks agreement expiry and cancellation deadlines.
     * 
     * Expiry: 2 months (60 days) before endDate → daily "ማደስ ወይም እንዲያድሱ" reminder.
     *         Past endDate → auto-expire and release property.
     * 
     * Cancellation: CANCELLATION_REQUESTED for 60+ days → auto-cancel.
     */
    @Scheduled(cron = "0 0 9 * * *")
    @Transactional
    public void checkAgreementLifecycles() {
        log.info("[AgreementLifecycle] Starting daily lifecycle check...");
        checkExpiringAgreements();
        checkCancellationDeadlines();
        log.info("[AgreementLifecycle] Daily lifecycle check completed.");
    }

    private void checkExpiringAgreements() {
        List<Agreement> activeAgreements = agreementRepository.findByStatus(AgreementStatus.ACTIVE);
        LocalDate today = LocalDate.now();
        int reminderCount = 0;
        int expiredCount = 0;

        for (Agreement agreement : activeAgreements) {
            try {
                if (agreement.getEndDate() == null) continue;
                
                LocalDate endDate = agreement.getEndDate().toLocalDate();
                long daysUntilEnd = ChronoUnit.DAYS.between(today, endDate);
                
                String tenantEmail = agreement.getTenant() != null ? agreement.getTenant().getEmail() : null;
                String landlordEmail = agreement.getLandlord() != null ? agreement.getLandlord().getEmail() : null;
                String agrNumber = agreement.getAgreementNumber();
                String propertyTitle = agreement.getProperty() != null ? agreement.getProperty().getPropertyCode() : agrNumber;

                if (daysUntilEnd <= 0) {
                    // Past end date — auto-expire
                    agreementService.expireAgreement(agreement.getId());
                    
                    String msg = String.format(
                            "ውሉ %s አብቅቷል። ንብረቱ %s አሁን ለኪራይ ተመልሷል።\n" +
                            "Agreement %s has expired. Property %s is now available.",
                            agrNumber, propertyTitle, agrNumber, propertyTitle);
                    sendNotification(tenantEmail, NotificationType.AGREEMENT_EXPIRED, "AGREEMENT", agrNumber, msg);
                    sendNotification(landlordEmail, NotificationType.AGREEMENT_EXPIRED, "AGREEMENT", agrNumber, msg);
                    expiredCount++;
                    
                } else if (daysUntilEnd >= 1 && daysUntilEnd <= 60) {
                    // 2 months to 1 day before expiry — send daily reminder
                    String msg = String.format(
                            "ውሉ %s ከ %d ቀን በኋላ ያበቃል። ማደስ ወይም እንዲያድሱ ይፈልጋሉ?\n" +
                            "Agreement %s expires in %d day(s) on %s. Would you like to renew?",
                            agrNumber, daysUntilEnd, agrNumber, daysUntilEnd, endDate);
                    sendNotification(tenantEmail, NotificationType.AGREEMENT_EXPIRY_REMINDER, "AGREEMENT", agrNumber, msg);
                    sendNotification(landlordEmail, NotificationType.AGREEMENT_EXPIRY_REMINDER, "AGREEMENT", agrNumber, msg);
                    reminderCount++;
                }
            } catch (Exception e) {
                log.warn("[AgreementLifecycle] Error processing expiry for {}: {}", 
                        agreement.getAgreementNumber(), e.getMessage());
            }
        }

        log.info("[AgreementLifecycle] Expiry check: {} reminders sent, {} agreements expired.", reminderCount, expiredCount);
    }

    private void checkCancellationDeadlines() {
        List<Agreement> cancellationRequested = agreementRepository.findByStatus(AgreementStatus.CANCELLATION_REQUESTED);
        LocalDateTime now = LocalDateTime.now();
        int autoCancelled = 0;

        for (Agreement agreement : cancellationRequested) {
            try {
                if (agreement.getCancellationRequestedAt() == null) continue;
                
                long daysSinceRequest = ChronoUnit.DAYS.between(
                        agreement.getCancellationRequestedAt().toLocalDate(), now.toLocalDate());
                
                String tenantEmail = agreement.getTenant() != null ? agreement.getTenant().getEmail() : null;
                String landlordEmail = agreement.getLandlord() != null ? agreement.getLandlord().getEmail() : null;
                String agrNumber = agreement.getAgreementNumber();

                if (daysSinceRequest >= 60) {
                    // 60 days reached — auto-cancel
                    agreementService.acceptCancellation(agrNumber, 
                            agreement.getTenant() != null ? agreement.getTenant().getEmail() : "system");
                    
                    String msg = String.format(
                            "ውል %s በ60 ቀን ውስጥ ተቀባይነት ስላልተገኘ በራስ-ሰር ተሰርዟል። ንብረቱ አሁን ለኪራይ ተመልሷል።\n" +
                            "Agreement %s has been auto-cancelled after 60 days without tenant acceptance. Property is now available.",
                            agrNumber, agrNumber);
                    sendNotification(tenantEmail, NotificationType.AGREEMENT_AUTO_CANCELLED, "AGREEMENT", agrNumber, msg);
                    sendNotification(landlordEmail, NotificationType.AGREEMENT_AUTO_CANCELLED, "AGREEMENT", agrNumber, msg);
                    autoCancelled++;
                }
            } catch (Exception e) {
                log.warn("[AgreementLifecycle] Error processing cancellation for {}: {}", 
                        agreement.getAgreementNumber(), e.getMessage());
            }
        }

        log.info("[AgreementLifecycle] Cancellation check: {} agreements auto-cancelled.", autoCancelled);
    }

    private void sendNotification(String recipientEmail, NotificationType type,
                                   String module, String entityId, String message) {
        if (recipientEmail == null) return;
        try {
            Notification notification = new Notification();
            notification.setRecipientUserId(recipientEmail);
            notification.setType(type);
            notification.setModule(module);
            notification.setEntityId(entityId);
            notification.setMessage(message);
            notification.setChannel(NotificationChannel.IN_APP);
            notification.setRead(false);
            notification.setCreatedAt(Instant.now());

            notificationRepository.save(notification);

            NotificationResponse response = notificationService.toNotificationResponse(notification);
            sseController.sendNotificationToUser(recipientEmail, response);
            sseController.sendUnreadCountUpdate(recipientEmail, notificationService.getUnreadCount(recipientEmail));
        } catch (Exception e) {
            log.debug("[AgreementLifecycle] Notification skipped for {}: {}", recipientEmail, e.getMessage());
        }
    }
}
