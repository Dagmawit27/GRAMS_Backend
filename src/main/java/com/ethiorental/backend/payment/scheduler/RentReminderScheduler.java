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
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
@Transactional
public class RentReminderScheduler {

    private final AgreementRepository agreementRepository;
    private final PaymentRepository paymentRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;
    private final NotificationSseController sseController;

    /**
     * Runs on application startup to ensure due dates and reminders are evaluated immediately.
     */
    @org.springframework.context.event.EventListener(org.springframework.boot.context.event.ApplicationReadyEvent.class)
    public void onStartup() {
        log.info("[RentReminder] Running startup state reconciliation and rent due date check...");
        try {
            reconcileAgreements();
        } catch (Exception e) {
            log.warn("[RentReminder] Agreement reconciliation encountered error: {}", e.getMessage());
        }
        try {
            checkRentDueDates();
        } catch (Exception e) {
            log.warn("[RentReminder] Rent due date check on startup encountered error: {}", e.getMessage());
        }
    }

    /**
     * Backfills totalMonthsPaid, paidThroughDate, and nextPaymentDueDate for existing agreements.
     */
    public void reconcileAgreements() {
        List<Agreement> activeAgreements = agreementRepository.findByStatusWithDetails(AgreementStatus.ACTIVE);
        for (Agreement a : activeAgreements) {
            LocalDate start = a.getStartDate() != null
                    ? a.getStartDate().toLocalDate()
                    : (a.getContractDate() != null ? a.getContractDate() : LocalDate.now());

            if (a.getMonthlyPaymentDueDay() == null) {
                a.setMonthlyPaymentDueDay(start.getDayOfMonth());
            }

            int advMonths = a.getAdvancePaymentMonths() != null && a.getAdvancePaymentMonths() > 0
                    ? a.getAdvancePaymentMonths() : 1;

            if (a.getTotalMonthsPaid() == null || a.getTotalMonthsPaid() == 0) {
                List<com.ethiorental.backend.payment.entity.Payment> payments = paymentRepository.findByAgreementId(a.getId());
                long completedPayments = payments.stream()
                        .filter(p -> p.getStatus() == com.ethiorental.backend.payment.enums.PaymentStatus.COMPLETED)
                        .count();
                if (completedPayments > 0) {
                    int totalMonths = advMonths + (int)(completedPayments - 1);
                    a.setTotalMonthsPaid(totalMonths);
                    LocalDate paidThrough = start.plusMonths(totalMonths);
                    a.setPaidThroughDate(paidThrough);
                    // Next payment is due the day after paid-through date
                    LocalDate nextDue = paidThrough.plusDays(1);
                    a.setNextPaymentDueDate(nextDue);
                    agreementRepository.save(a);
                    log.info("[RentReminder] Reconciled agreement {}: totalMonthsPaid={}, paidThrough={}, nextDue={}",
                            a.getAgreementNumber(), totalMonths, paidThrough, nextDue);
                } else {
                    a.setTotalMonthsPaid(0);
                    a.setNextPaymentDueDate(start);
                    agreementRepository.save(a);
                }
            } else if (a.getNextPaymentDueDate() == null) {
                LocalDate paidThrough = a.getPaidThroughDate() != null
                    ? a.getPaidThroughDate()
                    : start.plusMonths(a.getTotalMonthsPaid());
                a.setPaidThroughDate(paidThrough);
                // Next payment is due the day after paid-through date
                LocalDate nextDue = paidThrough.plusDays(1);
                a.setNextPaymentDueDate(nextDue);
                agreementRepository.save(a);
            }
        }
    }

    /**
     * Runs daily at 08:00 AM and every 10 minutes to check rent payment due dates.
     * Sends reminders 5 days to 1 day before due, and overdue alerts after.
     * Uses daily deduplication so each recipient gets at most one reminder/alert per day.
     */
    @Scheduled(cron = "0 0 8 * * *")
    @Scheduled(fixedRate = 600000, initialDelay = 15000)
    public void checkRentDueDates() {
        log.info("[RentReminder] Starting rent due date check...");

        List<Agreement> activeAgreements = agreementRepository.findByStatusWithDetails(AgreementStatus.ACTIVE);
        LocalDate today = LocalDate.now();
        Instant startOfDay = today.atStartOfDay(ZoneId.systemDefault()).toInstant();
        int remindersCount = 0;
        int overdueCount = 0;

        for (Agreement agreement : activeAgreements) {
            try {
                if (agreement.getStatus() != AgreementStatus.ACTIVE) continue;

                int advMonths = agreement.getAdvancePaymentMonths() != null && agreement.getAdvancePaymentMonths() > 0
                        ? agreement.getAdvancePaymentMonths() : 1;
                int monthsPaid = agreement.getTotalMonthsPaid() != null ? agreement.getTotalMonthsPaid() : 0;
                LocalDate nextDueDate = agreement.getNextPaymentDueDate();

                if (nextDueDate == null) {
                    LocalDate start = agreement.getStartDate() != null
                            ? agreement.getStartDate().toLocalDate()
                            : (agreement.getContractDate() != null ? agreement.getContractDate() : today);
                    nextDueDate = monthsPaid > 0 ? start.plusMonths(monthsPaid) : start;
                }

                long daysUntilDue = ChronoUnit.DAYS.between(today, nextDueDate);

                // If more than 5 days remain until next due date, tenant is in good standing and not in alert window
                if (daysUntilDue > 5) {
                    continue;
                }

                int nextMonthNum = monthsPaid < advMonths ? 1 : (monthsPaid + 1);
                String periodDescription = monthsPaid < advMonths
                        ? String.format("Advance Rent (%d Months)", advMonths)
                        : String.format("Month %d Rent", nextMonthNum);

                String tenantEmail = agreement.getTenant() != null ? agreement.getTenant().getEmail() : null;
                String landlordEmail = agreement.getLandlord() != null ? agreement.getLandlord().getEmail() : null;
                String propertyTitle = agreement.getProperty() != null && agreement.getProperty().getPropertyCode() != null
                        ? agreement.getProperty().getPropertyCode()
                        : (agreement.getProperty() != null && agreement.getProperty().getTitle() != null
                                ? agreement.getProperty().getTitle() : agreement.getAgreementNumber());
                String rentAmount = (monthsPaid < advMonths && agreement.getMonthlyRent() != null)
                        ? agreement.getMonthlyRent().multiply(java.math.BigDecimal.valueOf(advMonths)).toPlainString()
                        : (agreement.getMonthlyRent() != null ? agreement.getMonthlyRent().toPlainString() : "N/A");

                if (daysUntilDue >= 1 && daysUntilDue <= 5) {
                    // Send reminder: 5 down to 1 day left
                    String tenantMsg = String.format(
                            "የኪራይ ክፍያ ማስታወሻ: ለንብረት %s የ ETB %s (%s) ክፍያ በ %d ቀን ውስጥ ይደርሳል (በ%s)። እባክዎ በወቅቱ ይክፈሉ።\n" +
                            "Rent Payment Reminder: ETB %s for %s (%s) is due in %d day(s) on %s. Please pay on time.",
                            propertyTitle, rentAmount, periodDescription, daysUntilDue, nextDueDate,
                            rentAmount, propertyTitle, periodDescription, daysUntilDue, nextDueDate);

                    String landlordMsg = String.format(
                            "Tenant rent for %s (%s) is due in %d day(s) on %s. Expected amount: ETB %s.",
                            propertyTitle, periodDescription, daysUntilDue, nextDueDate, rentAmount);

                    if (sendNotificationIfNotSentToday(tenantEmail, NotificationType.RENT_PAYMENT_REMINDER,
                            "PAYMENT", agreement.getAgreementNumber(), tenantMsg, startOfDay)) {
                        remindersCount++;
                    }
                    sendNotificationIfNotSentToday(landlordEmail, NotificationType.RENT_PAYMENT_REMINDER,
                            "PAYMENT", agreement.getAgreementNumber(), landlordMsg, startOfDay);

                } else if (daysUntilDue <= 0) {
                    // Overdue alert (0 days = Due Today, negative days = Overdue)
                    long daysOverdue = Math.abs(daysUntilDue);
                    String tenantMsg = daysOverdue == 0
                            ? String.format(
                                "የኪራይ ክፍያ ማስታወሻ: ለንብረት %s የ ETB %s (%s) የዛሬ ክፍያ ቀን ነው። እባክዎ ክፍያውን ያጠናቅቁ።\n" +
                                "Rent Payment Alert: ETB %s for %s (%s) is due TODAY (%s). Please pay on time.",
                                propertyTitle, rentAmount, periodDescription, rentAmount, propertyTitle, periodDescription, nextDueDate)
                            : String.format(
                                "ያልተከፈለ ኪራይ ማስጠንቀቂያ: ለንብረት %s የ ETB %s (%s) ክፍያ %d ቀን አልፎታል! እባክዎ አሁኑኑ ይክፈሉ።\n" +
                                "OVERDUE Rent Alert: ETB %s for %s (%s) is %d day(s) overdue! Please pay immediately.",
                                propertyTitle, rentAmount, periodDescription, daysOverdue,
                                rentAmount, propertyTitle, periodDescription, daysOverdue);

                    String landlordMsg = daysOverdue == 0
                            ? String.format(
                                "DUE TODAY: Tenant rent of ETB %s for %s (%s) is due today (%s).",
                                rentAmount, propertyTitle, periodDescription, nextDueDate)
                            : String.format(
                                "OVERDUE: Tenant rent for %s (%s) is %d day(s) overdue. Expected amount: ETB %s.",
                                propertyTitle, periodDescription, daysOverdue, rentAmount);

                    if (sendNotificationIfNotSentToday(tenantEmail, NotificationType.RENT_PAYMENT_OVERDUE,
                            "PAYMENT", agreement.getAgreementNumber(), tenantMsg, startOfDay)) {
                        overdueCount++;
                    }
                    sendNotificationIfNotSentToday(landlordEmail, NotificationType.RENT_PAYMENT_OVERDUE,
                            "PAYMENT", agreement.getAgreementNumber(), landlordMsg, startOfDay);
                }
            } catch (Exception e) {
                log.warn("[RentReminder] Error processing agreement {}: {}",
                        agreement.getAgreementNumber(), e.getMessage());
            }
        }

        log.info("[RentReminder] Completed. Checked {} agreements. Sent {} reminders, {} overdue alerts.",
                activeAgreements.size(), remindersCount, overdueCount);
    }

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public boolean sendNotificationIfNotSentToday(String recipientEmail, NotificationType type,
                                                   String module, String entityId, String message,
                                                   Instant startOfDay) {
        if (recipientEmail == null || recipientEmail.isBlank()) return false;
        String email = recipientEmail.trim();

        // Daily deduplication check
        boolean alreadySent = notificationRepository.existsByRecipientAndTypeAndEntityIdSince(
                email, type, entityId, startOfDay);
        if (alreadySent) {
            log.debug("[RentReminder] Notification {} already sent today to {} for entity {}", type, email, entityId);
            return false;
        }

        try {
            Notification notification = new Notification();
            notification.setRecipientUserId(email);
            notification.setType(type);
            notification.setModule(module);
            notification.setEntityId(entityId);
            notification.setMessage(message);
            notification.setChannel(NotificationChannel.IN_APP);
            notification.setRead(false);
            notification.setCreatedAt(Instant.now());

            notificationRepository.save(notification);

            NotificationResponse response = notificationService.toNotificationResponse(notification);
            sseController.sendNotificationToUser(email, response);
            if (!email.equals(email.toLowerCase())) {
                sseController.sendNotificationToUser(email.toLowerCase(), response);
            }
            long unread = notificationService.getUnreadCount(email);
            sseController.sendUnreadCountUpdate(email, unread);
            if (!email.equals(email.toLowerCase())) {
                sseController.sendUnreadCountUpdate(email.toLowerCase(), unread);
            }
            return true;
        } catch (Exception e) {
            log.warn("[RentReminder] Failed to save/emit notification for {}: {}", email, e.getMessage());
            return false;
        }
    }
}
