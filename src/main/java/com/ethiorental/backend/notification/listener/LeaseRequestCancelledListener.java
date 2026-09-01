package com.ethiorental.backend.notification.listener;

import com.ethiorental.backend.notification.dto.response.NotificationResponse;
import com.ethiorental.backend.notification.entity.Notification;
import com.ethiorental.backend.notification.repository.NotificationRepository;
import com.ethiorental.backend.notification.service.NotificationService;
import com.ethiorental.backend.notification.controller.NotificationSseController;
import com.ethiorental.backend.lease.event.LeaseRequestCancelledEvent;
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
public class LeaseRequestCancelledListener {

    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;
    private final NotificationSseController sseController;

    @Async
    @EventListener
    @Transactional
    public void handleLeaseRequestCancelledEvent(LeaseRequestCancelledEvent event) {
        log.info("Handling LeaseRequestCancelledEvent for request: {}, landlordEmail: {}", 
                 event.getRequestCode(), event.getLandlordEmail());

        try {
            // Notify landlord
            String landlordUserId = event.getLandlordEmail();
            log.info("Notifying landlord: {}", landlordUserId);

            Notification notification = new Notification();
            notification.setRecipientUserId(landlordUserId);
            notification.setType(NotificationType.LEASE_CANCELLED);
            notification.setModule("LEASE");
            notification.setEntityId(event.getLeaseRequestId().toString());
            notification.setMessage(String.format(
                "Lease request %s for property %s (%s) has been cancelled by tenant %s.",
                event.getRequestCode(),
                event.getPropertyTitle(),
                event.getPropertyCode(),
                event.getTenantName()
            ));
            notification.setChannel(NotificationChannel.IN_APP);
            notification.setRead(false);
            notification.setCreatedAt(Instant.now());

            notificationRepository.save(notification);
            log.info("Successfully created notification for lease request cancellation: {}", event.getRequestCode());

            // Send via SSE
            NotificationResponse response = notificationService.toNotificationResponse(notification);
            sseController.sendNotificationToUser(landlordUserId, response);

            // Send unread count update
            long unreadCount = notificationService.getUnreadCount(landlordUserId);
            sseController.sendUnreadCountUpdate(landlordUserId, unreadCount);

            log.info("Sent SSE notification to landlord: {}", landlordUserId);
        } catch (Exception e) {
            log.error("Error handling LeaseRequestCancelledEvent for request: {}", event.getRequestCode(), e);
        }
    }
}
