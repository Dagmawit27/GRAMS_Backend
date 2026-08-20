package com.ethiorental.backend.notification.service;

import com.ethiorental.backend.IAM.entity.Citizen;
import com.ethiorental.backend.IAM.entity.GovernmentEmployee;
import com.ethiorental.backend.IAM.repository.CitizenRepository;
import com.ethiorental.backend.IAM.repository.GovernmentEmployeeRepository;
import com.ethiorental.backend.notification.entity.Notification;
import com.ethiorental.backend.notification.entity.NotificationPreference;
import com.ethiorental.backend.notification.entity.NotificationTemplate;
import com.ethiorental.backend.notification.exception.NotificationDeliveryException;
import com.ethiorental.backend.notification.repository.NotificationPreferenceRepository;
import com.ethiorental.backend.notification.repository.NotificationRepository;
import com.ethiorental.backend.notification.repository.NotificationTemplateRepository;
import com.ethiorental.backend.shared.notification.NotificationChannel;
import com.ethiorental.backend.shared.notification.NotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationListenerImpl implements NotificationListener {

    private final NotificationRepository notificationRepository;
    private final NotificationTemplateRepository templateRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final SmsGatewayAdapter smsGatewayAdapter;
    private final EmailAdapter emailAdapter;
    private final CitizenRepository citizenRepository;
    private final GovernmentEmployeeRepository employeeRepository;

    @Override
    public void onEvent(NotificationEvent event) {
        if (event == null || event.recipientUserId() == null) {
            log.warn("Cannot process NotificationEvent with missing recipient or event.");
            return;
        }

        String recipientId = event.recipientUserId();
        Set<NotificationChannel> channelsToSend = resolveChannels(recipientId, event);

        // 1. IN_APP Channel (Always active)
        if (channelsToSend.contains(NotificationChannel.IN_APP)) {
            deliverInApp(event);
        }

        // Resolve contact details for external channels (SMS, EMAIL)
        UserContact contact = resolveUserContact(recipientId);

        // 2. SMS Channel
        if (channelsToSend.contains(NotificationChannel.SMS)) {
            String smsBody = resolveMessageBody(event, NotificationChannel.SMS);
            String phone = contact.phone() != null ? contact.phone() : recipientId;
            try {
                smsGatewayAdapter.sendSms(recipientId, phone, smsBody, event.type());
            } catch (NotificationDeliveryException ex) {
                log.error("SMS delivery failed for channel [SMS] user [{}] type [{}] entity [{}]: {}",
                        ex.getRecipientUserId(), ex.getNotificationType(), event.entityId(), ex.getMessage());
            }
        }

        // 3. EMAIL Channel
        if (channelsToSend.contains(NotificationChannel.EMAIL)) {
            String emailBody = resolveMessageBody(event, NotificationChannel.EMAIL);
            String subject = resolveEmailSubject(event);
            String email = contact.email() != null ? contact.email() : recipientId;
            try {
                emailAdapter.sendEmail(recipientId, email, subject, emailBody, event.type());
            } catch (NotificationDeliveryException ex) {
                log.error("Email delivery failed for channel [EMAIL] user [{}] type [{}] entity [{}]: {}",
                        ex.getRecipientUserId(), ex.getNotificationType(), event.entityId(), ex.getMessage());
            }
        }
    }

    private Set<NotificationChannel> resolveChannels(String recipientUserId, NotificationEvent event) {
        Set<NotificationChannel> channels = new HashSet<>();
        channels.add(NotificationChannel.IN_APP); // Always included

        Set<NotificationChannel> publisherPreferred = event.preferredChannels();
        if (publisherPreferred == null || publisherPreferred.isEmpty()) {
            return channels;
        }

        Optional<NotificationPreference> userPrefOpt = preferenceRepository.findByUserIdAndType(recipientUserId, event.type());

        if (userPrefOpt.isPresent()) {
            Set<NotificationChannel> userEnabled = userPrefOpt.get().getEnabledChannels();
            if (userEnabled != null) {
                for (NotificationChannel channel : publisherPreferred) {
                    if (channel != NotificationChannel.IN_APP && userEnabled.contains(channel)) {
                        channels.add(channel);
                    }
                }
            }
        } else {
            // Default opt-in: include all publisher-preferred external channels when no preference row exists
            for (NotificationChannel channel : publisherPreferred) {
                if (channel != NotificationChannel.IN_APP) {
                    channels.add(channel);
                }
            }
        }

        return channels;
    }

    private void deliverInApp(NotificationEvent event) {
        String body = resolveMessageBody(event, NotificationChannel.IN_APP);
        Notification notification = Notification.builder()
                .recipientUserId(event.recipientUserId())
                .type(event.type())
                .module(event.module() != null ? event.module() : "GENERAL")
                .entityId(event.entityId())
                .message(body)
                .channel(NotificationChannel.IN_APP)
                .read(false)
                .build();

        notificationRepository.save(notification);
        log.debug("In-app notification created for user [{}]", event.recipientUserId());
    }

    private String resolveMessageBody(NotificationEvent event, NotificationChannel channel) {
        Optional<NotificationTemplate> templateOpt = templateRepository.findByTypeAndChannel(event.type(), channel);
        if (templateOpt.isPresent() && templateOpt.get().getBodyTemplate() != null) {
            String rawTemplate = templateOpt.get().getBodyTemplate();
            return formatTemplate(rawTemplate, event);
        }
        return event.message() != null ? event.message() : "Notification for " + event.type();
    }

    private String resolveEmailSubject(NotificationEvent event) {
        Optional<NotificationTemplate> templateOpt = templateRepository.findByTypeAndChannel(event.type(), NotificationChannel.EMAIL);
        if (templateOpt.isPresent() && templateOpt.get().getSubject() != null) {
            return formatTemplate(templateOpt.get().getSubject(), event);
        }
        return "GRAMS Notification: " + (event.type() != null ? event.type().name() : "Notice");
    }

    private String formatTemplate(String template, NotificationEvent event) {
        String result = template;
        if (event.entityId() != null) {
            result = result.replace("{entityId}", event.entityId());
        }
        if (event.message() != null) {
            result = result.replace("{message}", event.message());
        }
        if (event.module() != null) {
            result = result.replace("{module}", event.module());
        }
        return result;
    }

    private UserContact resolveUserContact(String userId) {
        try {
            UUID uuid = UUID.fromString(userId);
            Optional<Citizen> citizenOpt = citizenRepository.findById(uuid);
            if (citizenOpt.isPresent()) {
                return new UserContact(citizenOpt.get().getPhone(), citizenOpt.get().getEmail());
            }
            Optional<GovernmentEmployee> empOpt = employeeRepository.findById(uuid);
            if (empOpt.isPresent()) {
                return new UserContact(empOpt.get().getPhone(), empOpt.get().getEmail());
            }
        } catch (IllegalArgumentException e) {
            // Not a UUID, try email or username lookup
            Optional<Citizen> citizenOpt = citizenRepository.findByEmail(userId);
            if (citizenOpt.isPresent()) {
                return new UserContact(citizenOpt.get().getPhone(), citizenOpt.get().getEmail());
            }
            Optional<GovernmentEmployee> empOpt = employeeRepository.findByEmail(userId);
            if (empOpt.isPresent()) {
                return new UserContact(empOpt.get().getPhone(), empOpt.get().getEmail());
            }
        }
        return new UserContact(null, null);
    }

    private record UserContact(String phone, String email) {}
}
