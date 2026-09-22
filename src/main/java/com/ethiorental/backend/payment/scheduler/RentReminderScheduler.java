package com.ethiorental.backend.payment.scheduler;

import com.ethiorental.backend.agreement.entity.Agreement;
import com.ethiorental.backend.agreement.enums.AgreementStatus;
import com.ethiorental.backend.agreement.repository.AgreementRepository;
import com.ethiorental.backend.notification.controller.NotificationSseController;
import com.ethiorental.backend.notification.dto.response.NotificationResponse;
import com.ethiorental.backend.notification.entity.Notification;
import com.ethiorental.backend.notification.repository.NotificationRepository;
import com.ethiorental.backend.notification.service.NotificationService;
import com.ethiorental.backend.payment.repository.PaymentRepository;
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
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class RentReminderScheduler {

    private final AgreementRepository agreementRepository;
    private final PaymentRepository paymentRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;
    private final NotificationSseController sseController;

    /**
     * Runs every day at 08:00 AM to check rent payment due dates.
     * Sends reminders 5 days to 1 day before due, and overdue alerts after.
     * Alerts persist (re-sent daily) until the tenant pays (COMPLETED).
     */
    @Scheduled(cron = "0 0 8 * * *")
    @Transactional(readOnly = true)
    public void checkRentDueDates() {
        log.info("[RentReminder] Starting daily rent due date check...");

        List<Agreement> activeAgreements = agreementRepository.findByStatus(AgreementStatus.ACTIVE);
        LocalDate today = LocalDate.now();
        int remindersCount = 0;
        int overdueCount = 0;

        for (Agreement agreement : activeAgreements) {
            try {
                int dueDay = agreement.getMonthlyPaymentDueDay() != null
                        ? agreement.getMonthlyPaymentDueDay() : 5;

                // Calculate next due date
                LocalDate nextDueDate = calculateNextDueDate(today, dueDay);

                // Define the billing period for this due date
                LocalDateTime periodStart = nextDueDate.minusMonths(1).atStartOfDay();
                LocalDateTime periodEnd = nextDueDate.atTime(LocalTime.MAX);

                // Check if a COMPLETED payment already exists for this period
                boolean isPaid = paymentRepository.existsCompletedPaymentForPeriod(
                        agreement.getId(), periodStart, periodEnd);

                if (isPaid) {
                    continue; // Already paid, no reminder needed
                }

                long daysUntilDue = ChronoUnit.DAYS.between(today, nextDueDate);

                String tenantEmail = agreement.getTenant() != null ? agreement.getTenant().getEmail() : null;
                String landlordEmail = agreement.getLandlord() != null ? agreement.getLandlord().getEmail() : null;
                String propertyTitle = agreement.getProperty() != null ? agreement.getProperty().getPropertyCode() : agreement.getAgreementNumber();
                String monthlyRent = agreement.getMonthlyRent() != null ? agreement.getMonthlyRent().toPlainString() : "N/A";

                if (daysUntilDue >= 1 && daysUntilDue <= 5) {
                    // Send reminder: 5 down to 1 day left
                    String msg = String.format(
                            "የኪራይ ክፍያ ማስታወሻ: ለንብረት %s የ ETB %s ክፍያ በ %d ቀን ውስጥ ይደርሳል (በ%s)። እባክዎ በወቅቱ ይክፈሉ።\n" +
                            "Rent Payment Reminder: ETB %s rent for %s is due in %d day(s) on %s. Please pay on time.",
                            propertyTitle, monthlyRent, daysUntilDue, nextDueDate,
                            monthlyRent, propertyTitle, daysUntilDue, nextDueDate);

                    sendNotification(tenantEmail, NotificationType.RENT_PAYMENT_REMINDER,
                            "PAYMENT", agreement.getAgreementNumber(), msg);
                    sendNotification(landlordEmail, NotificationType.RENT_PAYMENT_REMINDER,
                            "PAYMENT", agreement.getAgreementNumber(),
                            String.format("Tenant rent for %s is due in %d day(s). Amount: ETB %s.",
                                    propertyTitle, daysUntilDue, monthlyRent));
                    remindersCount++;

                } else if (daysUntilDue < 0) {
                    // Overdue alert
                    long daysOverdue = Math.abs(daysUntilDue);
                    String msg = String.format(
                            "ያልተከፈለ ኪራይ ማስጠንቀቂያ: ለንብረት %s የ ETB %s ኪራይ ክፍያ %d ቀን አልፎታል! እባክዎ አሁኑኑ ይክፈሉ።\n" +
                            "OVERDUE Rent Alert: ETB %s rent for %s is %d day(s) overdue! Please pay immediately.",
                            propertyTitle, monthlyRent, daysOverdue,
                            monthlyRent, propertyTitle, daysOverdue);

                    sendNotification(tenantEmail, NotificationType.RENT_PAYMENT_OVERDUE,
                            "PAYMENT", agreement.getAgreementNumber(), msg);
                    sendNotification(landlordEmail, NotificationType.RENT_PAYMENT_OVERDUE,
                            "PAYMENT", agreement.getAgreementNumber(),
                            String.format("OVERDUE: Tenant rent for %s is %d day(s) overdue. Amount: ETB %s.",
                                    propertyTitle, daysOverdue, monthlyRent));
                    overdueCount++;
                }
            } catch (Exception e) {
                log.warn("[RentReminder] Error processing agreement {}: {}",
                        agreement.getAgreementNumber(), e.getMessage());
            }
        }

        log.info("[RentReminder] Completed. Checked {} agreements. Sent {} reminders, {} overdue alerts.",
                activeAgreements.size(), remindersCount, overdueCount);
    }

    private LocalDate calculateNextDueDate(LocalDate today, int dueDay) {
        int maxDay = today.lengthOfMonth();
        int clampedDay = Math.min(dueDay, maxDay);
        LocalDate thisMonthDue = today.withDayOfMonth(clampedDay);

        if (today.isAfter(thisMonthDue.plusDays(30))) {
            LocalDate nextMonth = today.plusMonths(1);
            int nextMaxDay = nextMonth.lengthOfMonth();
            return nextMonth.withDayOfMonth(Math.min(dueDay, nextMaxDay));
        }

        return thisMonthDue;
    }

    private void sendNotification(String recipientEmail, NotificationType type,
                                   String module, String entityId, String message) {
        if (recipientEmail == null || recipientEmail.isBlank()) return;
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
            log.debug("[RentReminder] Notification emission skipped for {}: {}", recipientEmail, e.getMessage());
        }
    }
}
