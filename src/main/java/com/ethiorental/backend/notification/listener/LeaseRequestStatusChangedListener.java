package com.ethiorental.backend.notification.listener;

import com.ethiorental.backend.notification.dto.response.NotificationResponse;
import com.ethiorental.backend.notification.entity.Notification;
import com.ethiorental.backend.notification.repository.NotificationRepository;
import com.ethiorental.backend.notification.service.NotificationService;
import com.ethiorental.backend.notification.controller.NotificationSseController;
import com.ethiorental.backend.lease.event.LeaseRequestStatusChangedEvent;
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
public class LeaseRequestStatusChangedListener {

    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;
    private final NotificationSseController sseController;

    @Async
    @EventListener
    @Transactional
    public void handleLeaseRequestStatusChangedEvent(LeaseRequestStatusChangedEvent event) {
        log.info("Handling LeaseRequestStatusChangedEvent for request: {} from {} to {}", 
                 event.getRequestCode(), event.getOldStatus(), event.getNewStatus());

        try {
            // Notify tenant
            String tenantUserId = event.getTenantEmail();
            log.info("Notifying tenant: {}", tenantUserId);

            String message;
            if ("ACCEPTED".equalsIgnoreCase(event.getNewStatus())) {
                message = String.format(
                    "Your lease request for %s (%s) has been accepted by %s. Please proceed with agreement signing.",
                    event.getPropertyTitle(),
                    event.getPropertyCode(),
                    event.getLandlordName()
                );
            } else if ("DECLINED".equalsIgnoreCase(event.getNewStatus())) {
                message = String.format(
                    "Your lease request for %s (%s) has been declined by %s.",
                    event.getPropertyTitle(),
                    event.getPropertyCode(),
                    event.getLandlordName()
                );
            } else {
                message = String.format(
                    "Your lease request for %s (%s) status has been updated from %s to %s by %s.",
                    event.getPropertyTitle(),
                    event.getPropertyCode(),
                    event.getOldStatus(),
                    event.getNewStatus(),
                    event.getLandlordName()
                );
            }

            Notification notification = new Notification();
            notification.setRecipientUserId(tenantUserId);
            notification.setType(NotificationType.AGREEMENT_SIGNED);
            notification.setModule("LEASE");
            notification.setEntityId(event.getLeaseRequestId().toString());
            notification.setMessage(message);
            notification.setChannel(NotificationChannel.IN_APP);
            notification.setRead(false);
            notification.setCreatedAt(Instant.now());

            notificationRepository.save(notification);
            log.info("Notification saved for userId: {}", tenantUserId);

            // Send via SSE
            NotificationResponse response = notificationService.toNotificationResponse(notification);
            sseController.sendNotificationToUser(tenantUserId, response);

            // Send unread count update
            long unreadCount = notificationService.getUnreadCount(tenantUserId);
            sseController.sendUnreadCountUpdate(tenantUserId, unreadCount);

            log.info("Successfully created notification for lease request status change: {}", event.getRequestCode());
        } catch (Exception e) {
            log.error("Error handling LeaseRequestStatusChangedEvent for request: {}", event.getRequestCode(), e);
        }
    }
}
