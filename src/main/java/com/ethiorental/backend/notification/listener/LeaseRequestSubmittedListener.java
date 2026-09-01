package com.ethiorental.backend.notification.listener;

import com.ethiorental.backend.notification.dto.response.NotificationResponse;
import com.ethiorental.backend.notification.entity.Notification;
import com.ethiorental.backend.notification.repository.NotificationRepository;
import com.ethiorental.backend.notification.service.NotificationService;
import com.ethiorental.backend.notification.controller.NotificationSseController;
import com.ethiorental.backend.lease.event.LeaseRequestSubmittedEvent;
import com.ethiorental.backend.shared.notification.NotificationChannel;
import com.ethiorental.backend.shared.notification.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class LeaseRequestSubmittedListener {

    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;
    private final NotificationSseController sseController;

    @Async
    @EventListener
    @Transactional
    public void handleLeaseRequestSubmittedEvent(LeaseRequestSubmittedEvent event) {
        log.error("========== LeaseRequestSubmittedListener TRIGGERED ==========");
        log.error("Request Code: {}", event.getRequestCode());
        log.error("Landlord Email: {}", event.getLandlordEmail());
        log.error("Tenant Name: {}", event.getTenantName());
        log.error("Property Title: {}", event.getPropertyTitle());
        log.error("===========================================================");

        try {
            // Notify landlord
            String landlordUserId = event.getLandlordEmail();
            log.info("Notifying landlord: {}", landlordUserId);

            Notification notification = new Notification();
            notification.setRecipientUserId(landlordUserId);
            notification.setType(NotificationType.AGREEMENT);
            notification.setModule("LEASE");
            notification.setEntityId(event.getLeaseRequestId().toString());
            notification.setMessage(String.format(
                "New lease request from %s (%s) for property %s (%s). Monthly rent: ETB %s",
                event.getTenantName(),
                event.getTenantPhone(),
                event.getPropertyTitle(),
                event.getPropertyCode(),
                event.getMonthlyRent()
            ));
            notification.setChannel(NotificationChannel.IN_APP);
            notification.setRead(false);
            notification.setCreatedAt(Instant.now());

            notificationRepository.save(notification);
            log.info("Notification saved for userId: {}", landlordUserId);

            // Send via SSE
            NotificationResponse response = notificationService.toNotificationResponse(notification);
            sseController.sendNotificationToUser(landlordUserId, response);

            // Send unread count update
            long unreadCount = notificationService.getUnreadCount(landlordUserId);
            sseController.sendUnreadCountUpdate(landlordUserId, unreadCount);

            log.info("Successfully created notification for lease request submission: {}", event.getRequestCode());
        } catch (Exception e) {
            log.error("Error handling LeaseRequestSubmittedEvent for request: {}", event.getRequestCode(), e);
        }
    }
}
