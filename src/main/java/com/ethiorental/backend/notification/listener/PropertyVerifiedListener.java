package com.ethiorental.backend.notification.listener;

import com.ethiorental.backend.notification.dto.response.NotificationResponse;
import com.ethiorental.backend.notification.entity.Notification;
import com.ethiorental.backend.notification.entity.NotificationPreference;
import java.util.List;
import com.ethiorental.backend.notification.repository.NotificationRepository;
import com.ethiorental.backend.notification.repository.NotificationPreferenceRepository;
import com.ethiorental.backend.notification.service.NotificationService;
import com.ethiorental.backend.notification.controller.NotificationSseController;
import com.ethiorental.backend.property.event.PropertyVerifiedEvent;
import com.ethiorental.backend.shared.notification.NotificationChannel;
import com.ethiorental.backend.shared.notification.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class PropertyVerifiedListener {

    private final NotificationRepository notificationRepository;
    private final NotificationPreferenceRepository notificationPreferenceRepository;
    private final NotificationService notificationService;
    private final NotificationSseController sseController;

    @Async
    @EventListener
    @Transactional
    public void handlePropertyVerifiedEvent(PropertyVerifiedEvent event) {
        log.info("Handling PropertyVerifiedEvent for property: {}", event.getPropertyCode());

        try {
            // Notify woreda_supervisor
            String woredaSupervisorUserId = "woreda-supervisor-" + event.getWoreda();
            log.info("Notifying woreda_supervisor: {}", woredaSupervisorUserId);
            sendNotificationToUser(woredaSupervisorUserId, event, createSupervisorNotificationMessage(event));

            // Notify landlord
            String landlordUserId = event.getLandlordEmail();
            log.info("Notifying landlord: {}", landlordUserId);
            sendNotificationToUser(landlordUserId, event, createLandlordNotificationMessage(event));

            log.info("Successfully created notifications for property verification: {}", event.getPropertyCode());
        } catch (Exception e) {
            log.error("Error handling PropertyVerifiedEvent for property: {}", event.getPropertyCode(), e);
        }
    }

    private void sendNotificationToUser(String userId, PropertyVerifiedEvent event, String message) {
        Notification notification = new Notification();
        notification.setRecipientUserId(userId);
        notification.setType(NotificationType.PROPERTY_VERIFIED);
        notification.setModule("PROPERTY");
        notification.setEntityId(event.getPropertyId().toString());
        notification.setMessage(message);
        notification.setChannel(NotificationChannel.IN_APP);
        notification.setRead(false);
        notification.setCreatedAt(Instant.now());

        notificationRepository.save(notification);
        log.info("Notification saved for userId: {}", userId);

        // Send via SSE
        NotificationResponse response = notificationService.toNotificationResponse(notification);
        sseController.sendNotificationToUser(userId, response);

        // Send unread count update
        long unreadCount = notificationService.getUnreadCount(userId);
        sseController.sendUnreadCountUpdate(userId, unreadCount);
    }

    private String createSupervisorNotificationMessage(PropertyVerifiedEvent event) {
        String message = String.format(
            "Property verified by %s: %s (%s) in %s, %s, Woreda %s. Ready for final approval.",
            event.getWoredaOfficerName(),
            event.getPropertyTitle(),
            event.getPropertyCode(),
            event.getSubCity(),
            event.getCity(),
            event.getWoreda()
        );
        if (event.getVerificationNotes() != null && !event.getVerificationNotes().isBlank()) {
            message += " Notes: " + event.getVerificationNotes();
        }
        return message;
    }

    private String createLandlordNotificationMessage(PropertyVerifiedEvent event) {
        return String.format(
            "Your property %s (%s) has been verified by %s. It is now pending final approval.",
            event.getPropertyTitle(),
            event.getPropertyCode(),
            event.getWoredaOfficerName()
        );
    }
}
