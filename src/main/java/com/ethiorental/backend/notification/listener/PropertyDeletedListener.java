package com.ethiorental.backend.notification.listener;

import com.ethiorental.backend.notification.dto.response.NotificationResponse;
import com.ethiorental.backend.notification.entity.Notification;
import com.ethiorental.backend.notification.repository.NotificationRepository;
import com.ethiorental.backend.notification.service.NotificationService;
import com.ethiorental.backend.notification.controller.NotificationSseController;
import com.ethiorental.backend.property.event.PropertyDeletedEvent;
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
public class PropertyDeletedListener {

    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;
    private final NotificationSseController sseController;

    @Async
    @EventListener
    @Transactional
    public void handlePropertyDeletedEvent(PropertyDeletedEvent event) {
        log.info("Handling PropertyDeletedEvent for property: {}", event.getPropertyCode());

        try {
            // Notify woreda_officer for the property's location
            String woredaOfficerUserId = "woreda-officer-" + event.getWoreda();
            log.info("Notifying woreda_officer: {}", woredaOfficerUserId);

            Notification notification = new Notification();
            notification.setRecipientUserId(woredaOfficerUserId);
            notification.setType(NotificationType.PROPERTY_DELETED);
            notification.setModule("PROPERTY");
            notification.setEntityId(event.getPropertyId().toString());
            notification.setMessage(String.format(
                "Property deleted: %s (%s) in %s, %s, Woreda %s",
                event.getPropertyTitle(),
                event.getPropertyCode(),
                event.getSubCity(),
                event.getCity(),
                event.getWoreda()
            ));
            notification.setChannel(NotificationChannel.IN_APP);
            notification.setRead(false);
            notification.setCreatedAt(Instant.now());

            notificationRepository.save(notification);
            log.info("Notification saved for userId: {}", woredaOfficerUserId);

            // Send via SSE
            NotificationResponse response = notificationService.toNotificationResponse(notification);
            sseController.sendNotificationToUser(woredaOfficerUserId, response);

            // Send unread count update
            long unreadCount = notificationService.getUnreadCount(woredaOfficerUserId);
            sseController.sendUnreadCountUpdate(woredaOfficerUserId, unreadCount);

            log.info("Successfully created notification for property deletion: {}", event.getPropertyCode());
        } catch (Exception e) {
            log.error("Error handling PropertyDeletedEvent for property: {}", event.getPropertyCode(), e);
        }
    }
}
