package com.ethiorental.backend.notification.listener;

import com.ethiorental.backend.notification.dto.response.NotificationResponse;
import com.ethiorental.backend.notification.entity.Notification;
import com.ethiorental.backend.notification.repository.NotificationRepository;
import com.ethiorental.backend.notification.service.NotificationService;
import com.ethiorental.backend.notification.controller.NotificationSseController;
import com.ethiorental.backend.lease.event.LeaseRequestVerifiedEvent;
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
public class LeaseRequestVerifiedListener {

    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;
    private final NotificationSseController sseController;

    @Async
    @EventListener
    @Transactional
    public void handleLeaseRequestVerifiedEvent(LeaseRequestVerifiedEvent event) {
        log.info("Handling LeaseRequestVerifiedEvent for request: {}", event.getRequestCode());

        try {
            // 1. Notify supervisor channel for this woreda
            if (event.getWoreda() != null && !event.getWoreda().isBlank()) {
                String woredaSupervisorUserId = "woreda-supervisor-" + event.getWoreda();
                String supervisorMsg = String.format(
                    "Lease agreement for %s (%s) verified by officer %s and is pending your approval.",
                    event.getPropertyTitle(),
                    event.getPropertyCode(),
                    event.getOfficerName() != null ? event.getOfficerName() : "Officer"
                );

                log.info("Notifying woreda supervisor channel: {}", woredaSupervisorUserId);
                Notification supervisorNotification = createNotification(
                    woredaSupervisorUserId,
                    NotificationType.TASK_PENDING_FOR_OFFICER,
                    event.getLeaseRequestId().toString(),
                    supervisorMsg
                );
                notificationRepository.save(supervisorNotification);

                NotificationResponse supervisorResp = notificationService.toNotificationResponse(supervisorNotification);
                sseController.sendNotificationToUser(woredaSupervisorUserId, supervisorResp);
                sseController.sendUnreadCountUpdate(woredaSupervisorUserId, notificationService.getUnreadCount(woredaSupervisorUserId));
            }

            // 2. Notify tenant and landlord that the agreement has been verified
            String[] recipients = { event.getTenantEmail(), event.getLandlordEmail() };
            
            NotificationResponse userResponse = null;
            for (String recipientUserId : recipients) {
                if (recipientUserId == null || recipientUserId.isBlank()) continue;

                String message = String.format(
                    "Your lease agreement for %s (%s) has been verified by the officer and is pending supervisor approval.",
                    event.getPropertyTitle(),
                    event.getPropertyCode()
                );

                log.info("Notifying user: {}", recipientUserId);
                Notification notification = createNotification(
                    recipientUserId,
                    NotificationType.LEASE_REQUEST_STATUS_CHANGED,
                    event.getLeaseRequestId().toString(),
                    message
                );
                notificationRepository.save(notification);

                NotificationResponse response = notificationService.toNotificationResponse(notification);
                userResponse = response;
                sseController.sendNotificationToUser(recipientUserId, response);
                sseController.sendUnreadCountUpdate(recipientUserId, notificationService.getUnreadCount(recipientUserId));
            }

            // 3. Broadcast to requestCode and leaseRequestId channels for any active agreement detail views
            if (userResponse != null) {
                if (event.getRequestCode() != null && !event.getRequestCode().isBlank()) {
                    sseController.sendNotificationToUser(event.getRequestCode(), userResponse);
                }
                if (event.getLeaseRequestId() != null) {
                    sseController.sendNotificationToUser(event.getLeaseRequestId().toString(), userResponse);
                }
            }

            log.info("Successfully created notifications for lease request verified: {}", event.getRequestCode());
        } catch (Exception e) {
            log.error("Error handling LeaseRequestVerifiedEvent for request: {}", event.getRequestCode(), e);
        }
    }

    private Notification createNotification(String recipientUserId, NotificationType type, String entityId, String message) {
        Notification notification = new Notification();
        notification.setRecipientUserId(recipientUserId);
        notification.setType(type);
        notification.setModule("LEASE");
        notification.setEntityId(entityId);
        notification.setMessage(message);
        notification.setChannel(NotificationChannel.IN_APP);
        notification.setRead(false);
        notification.setCreatedAt(Instant.now());
        return notification;
    }
}
