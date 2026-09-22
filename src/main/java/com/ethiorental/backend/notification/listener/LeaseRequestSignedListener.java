package com.ethiorental.backend.notification.listener;

import com.ethiorental.backend.notification.dto.response.NotificationResponse;
import com.ethiorental.backend.notification.entity.Notification;
import com.ethiorental.backend.notification.repository.NotificationRepository;
import com.ethiorental.backend.notification.service.NotificationService;
import com.ethiorental.backend.notification.controller.NotificationSseController;
import com.ethiorental.backend.lease.event.LeaseRequestSignedEvent;
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
public class LeaseRequestSignedListener {

    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;
    private final NotificationSseController sseController;

    @Async
    @EventListener
    @Transactional
    public void handleLeaseRequestSignedEvent(LeaseRequestSignedEvent event) {
        log.info("Handling LeaseRequestSignedEvent for request: {} by {}, status: {}, bothSigned: {}", 
                 event.getRequestCode(), event.getSignerType(), event.getLeaseRequestStatus(), event.isBothSigned());

        try {
            boolean isUnderVerification = event.isBothSigned() || "UNDER_VERIFICATION".equalsIgnoreCase(event.getLeaseRequestStatus());

            if (isUnderVerification) {
                // 1. Notify the Woreda Officer responsible for this property's woreda
                String woredaOfficerUserId = "woreda-officer-" + event.getWoreda();
                String officerMessage = String.format(
                    "New lease agreement under verification: %s (%s) in SubCity %s, Woreda %s signed by both parties.",
                    event.getPropertyTitle(),
                    event.getPropertyCode(),
                    event.getSubCity(),
                    event.getWoreda()
                );

                log.info("Notifying woreda officer channel: {}", woredaOfficerUserId);
                Notification officerNotification = createNotification(
                    woredaOfficerUserId,
                    NotificationType.TASK_PENDING_FOR_OFFICER,
                    event.getLeaseRequestId().toString(),
                    officerMessage
                );
                notificationRepository.save(officerNotification);

                NotificationResponse officerResponse = notificationService.toNotificationResponse(officerNotification);
                sseController.sendNotificationToUser(woredaOfficerUserId, officerResponse);
                sseController.sendUnreadCountUpdate(woredaOfficerUserId, notificationService.getUnreadCount(woredaOfficerUserId));

                // 2. Notify Tenant that agreement is under verification
                String tenantMsg = String.format(
                    "Both parties have signed the lease agreement for %s (%s). It is now submitted for Woreda Officer verification.",
                    event.getPropertyTitle(),
                    event.getPropertyCode()
                );
                sendNotificationToUser(event.getTenantEmail(), NotificationType.LEASE_REQUEST_SIGNED, event.getLeaseRequestId().toString(), tenantMsg);

                // 3. Notify Landlord that agreement is under verification
                String landlordMsg = String.format(
                    "Both parties have signed the lease agreement for %s (%s). It is now submitted for Woreda Officer verification.",
                    event.getPropertyTitle(),
                    event.getPropertyCode()
                );
                sendNotificationToUser(event.getLandlordEmail(), NotificationType.LEASE_REQUEST_SIGNED, event.getLeaseRequestId().toString(), landlordMsg);

            } else {
                // Only one party has signed so far
                if ("landlord".equals(event.getSignerType())) {
                    // Notify tenant that landlord has signed
                    String message = String.format(
                        "Landlord %s has signed the lease agreement for %s (%s). Please review and sign the agreement.",
                        event.getLandlordName(),
                        event.getPropertyTitle(),
                        event.getPropertyCode()
                    );
                    sendNotificationToUser(event.getTenantEmail(), NotificationType.LEASE_REQUEST_SIGNED, event.getLeaseRequestId().toString(), message);
                } else if ("tenant".equals(event.getSignerType())) {
                    // Notify landlord that tenant has signed
                    String message = String.format(
                        "Tenant %s has signed the lease agreement for %s (%s).",
                        event.getTenantName(),
                        event.getPropertyTitle(),
                        event.getPropertyCode()
                    );
                    sendNotificationToUser(event.getLandlordEmail(), NotificationType.LEASE_REQUEST_SIGNED, event.getLeaseRequestId().toString(), message);
                }
            }

            log.info("Successfully processed LeaseRequestSignedEvent for request: {}", event.getRequestCode());
        } catch (Exception e) {
            log.error("Error handling LeaseRequestSignedEvent for request: {}", event.getRequestCode(), e);
        }
    }

    private void sendNotificationToUser(String recipientUserId, NotificationType type, String entityId, String message) {
        if (recipientUserId == null || recipientUserId.isBlank()) return;

        Notification notification = createNotification(recipientUserId, type, entityId, message);
        notificationRepository.save(notification);

        NotificationResponse response = notificationService.toNotificationResponse(notification);
        sseController.sendNotificationToUser(recipientUserId, response);

        long unreadCount = notificationService.getUnreadCount(recipientUserId);
        sseController.sendUnreadCountUpdate(recipientUserId, unreadCount);
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
