package com.ethiorental.backend.notification.listener;

import com.ethiorental.backend.notification.controller.NotificationSseController;
import com.ethiorental.backend.notification.entity.Notification;
import com.ethiorental.backend.notification.entity.NotificationPreference;
import java.util.List;
import com.ethiorental.backend.notification.repository.NotificationRepository;
import com.ethiorental.backend.notification.repository.NotificationPreferenceRepository;
import com.ethiorental.backend.notification.dto.response.NotificationResponse;
import com.ethiorental.backend.notification.service.NotificationService;
import com.ethiorental.backend.property.event.PropertyRegisteredEvent;
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
public class PropertyRegisteredListener {

    private final NotificationRepository notificationRepository;
    private final NotificationPreferenceRepository notificationPreferenceRepository;
    private final NotificationService notificationService;
    private final NotificationSseController sseController;

    @Async
    @EventListener
    @Transactional
    public void handlePropertyRegisteredEvent(PropertyRegisteredEvent event) {
        log.info("Handling PropertyRegisteredEvent for property: {}, woreda: {}", event.getPropertyCode(), event.getWoreda());

        try {
            // Target woreda_officer for the property's location
            // In a real implementation, you would query for the specific woreda_officer user ID
            // based on the property's woreda and subcity
            String woredaOfficerUserId = "woreda-officer-" + event.getWoreda();
            log.info("Sending SSE notification to userId: {}", woredaOfficerUserId);
            
            // Check if the user has notification preferences for property registrations
            List<NotificationPreference> preferences = notificationPreferenceRepository
                .findByUserId(woredaOfficerUserId);

            // Determine which channels to use based on preferences
            // Default to in-app notifications if no preferences exist
            boolean shouldNotifyInApp = true;
            boolean shouldNotifyEmail = false;
            boolean shouldNotifySms = false;

            if (!preferences.isEmpty()) {
                // Check if any preference has the specific channels enabled
                for (NotificationPreference pref : preferences) {
                    if (pref.getEnabledChannels().contains(NotificationChannel.IN_APP)) {
                        shouldNotifyInApp = true;
                    }
                    if (pref.getEnabledChannels().contains(NotificationChannel.EMAIL)) {
                        shouldNotifyEmail = true;
                    }
                    if (pref.getEnabledChannels().contains(NotificationChannel.SMS)) {
                        shouldNotifySms = true;
                    }
                }
            }

            if (shouldNotifyInApp) {
                Notification notification = createNotification(
                    woredaOfficerUserId,
                    event,
                    NotificationChannel.IN_APP
                );
                notificationRepository.save(notification);
                
                log.info("Notification saved for userId: {}, propertyCode: {}", woredaOfficerUserId, event.getPropertyCode());
                
                // Convert to response DTO and send via SSE for real-time push
                NotificationResponse response = notificationService.toNotificationResponse(notification);
                log.info("Sending SSE notification to userId: {}", woredaOfficerUserId);
                sseController.sendNotificationToUser(woredaOfficerUserId, response);
                
                // Also send unread count update
                long unreadCount = notificationService.getUnreadCount(woredaOfficerUserId);
                log.info("Sending unread count update to userId: {}, count: {}", woredaOfficerUserId, unreadCount);
                sseController.sendUnreadCountUpdate(woredaOfficerUserId, unreadCount);
            }

            if (shouldNotifyEmail) {
                Notification notification = createNotification(
                    woredaOfficerUserId,
                    event,
                    NotificationChannel.EMAIL
                );
                notificationRepository.save(notification);
            }

            if (shouldNotifySms) {
                Notification notification = createNotification(
                    woredaOfficerUserId,
                    event,
                    NotificationChannel.SMS
                );
                notificationRepository.save(notification);
            }

            log.info("Successfully created notifications for property registration for woreda_officer: {}", event.getPropertyCode());
        } catch (Exception e) {
            log.error("Error handling PropertyRegisteredEvent for property: {}", event.getPropertyCode(), e);
        }
    }

    private Notification createNotification(String recipientUserId, PropertyRegisteredEvent event, NotificationChannel channel) {
        Notification notification = new Notification();
        // Don't manually set ID - let @UuidGenerator handle it
        notification.setRecipientUserId(recipientUserId);
        notification.setType(NotificationType.PROPERTY_REGISTERED);
        notification.setModule("PROPERTY");
        notification.setEntityId(event.getPropertyId().toString());
        
        String message = String.format(
            "New property registered: %s (%s) in %s, %s, Woreda %s by %s",
            event.getPropertyTitle(),
            event.getPropertyCode(),
            event.getSubCity(),
            event.getCity(),
            event.getWoreda(),
            event.getLandlordName()
        );
        notification.setMessage(message);
        notification.setChannel(channel);
        notification.setRead(false);
        notification.setCreatedAt(Instant.now());
        
        return notification;
    }
}
