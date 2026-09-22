package com.ethiorental.backend.notification.listener;

import com.ethiorental.backend.notification.dto.response.NotificationResponse;
import com.ethiorental.backend.notification.entity.Notification;
import com.ethiorental.backend.notification.repository.NotificationRepository;
import com.ethiorental.backend.notification.service.NotificationService;
import com.ethiorental.backend.notification.controller.NotificationSseController;
import com.ethiorental.backend.property.event.PropertyApprovedEvent;
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
public class PropertyApprovedListener {

    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;
    private final NotificationSseController sseController;

    @Async
    @EventListener
    @Transactional
    public void handlePropertyApprovedEvent(PropertyApprovedEvent event) {
        log.info("Handling PropertyApprovedEvent for property: {}, landlordEmail: {}", 
                 event.getPropertyCode(), event.getLandlordEmail());

        try {
            // Notify landlord
            String landlordUserId = event.getLandlordEmail();
            log.info("Notifying landlord: {}", landlordUserId);

            Notification notification = new Notification();
            notification.setRecipientUserId(landlordUserId);
            notification.setType(NotificationType.PROPERTY_APPROVED);
            notification.setModule("PROPERTY");
            notification.setEntityId(event.getPropertyId().toString());
            notification.setMessage(String.format(
                "Your property %s (%s) has been approved by %s. It is now listed and available for lease.",
                event.getPropertyTitle(),
                event.getPropertyCode(),
                event.getSupervisorName()
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

            log.info("Successfully created notification for property approval: {}", event.getPropertyCode());
        } catch (Exception e) {
            log.error("Error handling PropertyApprovedEvent for property: {}", event.getPropertyCode(), e);
        }
    }
}
