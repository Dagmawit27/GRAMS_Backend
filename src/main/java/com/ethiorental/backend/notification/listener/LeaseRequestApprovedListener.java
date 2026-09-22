package com.ethiorental.backend.notification.listener;

import com.ethiorental.backend.notification.dto.response.NotificationResponse;
import com.ethiorental.backend.notification.entity.Notification;
import com.ethiorental.backend.notification.repository.NotificationRepository;
import com.ethiorental.backend.notification.service.NotificationService;
import com.ethiorental.backend.notification.controller.NotificationSseController;
import com.ethiorental.backend.lease.event.LeaseRequestApprovedEvent;
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
public class LeaseRequestApprovedListener {

    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;
    private final NotificationSseController sseController;

    @Async
    @EventListener
    @Transactional
    public void handleLeaseRequestApprovedEvent(LeaseRequestApprovedEvent event) {
        log.info("Handling LeaseRequestApprovedEvent for request: {}", event.getRequestCode());

        try {
            // Notify tenant and landlord that the agreement has been approved
            String[] recipients = { event.getTenantEmail(), event.getLandlordEmail() };
            
            for (String recipientUserId : recipients) {
                String message = String.format(
                    "Your lease agreement for %s (%s) has been approved by the supervisor and is now active.",
                    event.getPropertyTitle(),
                    event.getPropertyCode()
                );

                log.info("Notifying user: {}", recipientUserId);

                Notification notification = new Notification();
                notification.setRecipientUserId(recipientUserId);
                notification.setType(NotificationType.LEASE_REQUEST_STATUS_CHANGED);
                notification.setModule("LEASE");
                notification.setEntityId(event.getLeaseRequestId().toString());
                notification.setMessage(message);
                notification.setChannel(NotificationChannel.IN_APP);
                notification.setRead(false);
                notification.setCreatedAt(Instant.now());

                notificationRepository.save(notification);
                log.info("Notification saved for userId: {}", recipientUserId);

                // Send via SSE
                NotificationResponse response = notificationService.toNotificationResponse(notification);
                sseController.sendNotificationToUser(recipientUserId, response);

                // Send unread count update
                long unreadCount = notificationService.getUnreadCount(recipientUserId);
                sseController.sendUnreadCountUpdate(recipientUserId, unreadCount);
            }

            log.info("Successfully created notifications for lease request approved: {}", event.getRequestCode());
        } catch (Exception e) {
            log.error("Error handling LeaseRequestApprovedEvent for request: {}", event.getRequestCode(), e);
        }
    }
}
