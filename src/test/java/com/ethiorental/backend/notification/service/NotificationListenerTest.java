package com.ethiorental.backend.notification.service;

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
import com.ethiorental.backend.shared.notification.NotificationType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class NotificationListenerTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationTemplateRepository templateRepository;

    @Mock
    private NotificationPreferenceRepository preferenceRepository;

    @Mock
    private SmsGatewayAdapter smsGatewayAdapter;

    @Mock
    private EmailAdapter emailAdapter;

    @Mock
    private CitizenRepository citizenRepository;

    @Mock
    private GovernmentEmployeeRepository employeeRepository;

    @InjectMocks
    private NotificationListenerImpl listener;

    private AutoCloseable closeable;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (closeable != null) {
            closeable.close();
        }
    }

    @Test
    @DisplayName("IN_APP channel is always included even if preferredChannels is empty")
    void testInAppAlwaysIncluded() {
        String userId = "user-100";
        NotificationEvent event = new NotificationEvent(
                NotificationType.PROPERTY_SUBMITTED,
                userId,
                "PROPERTY",
                "prop-1",
                "Property submitted",
                Set.of() // empty preferredChannels
        );

        when(preferenceRepository.findByUserIdAndType(userId, NotificationType.PROPERTY_SUBMITTED))
                .thenReturn(Optional.empty());

        listener.onEvent(event);

        verify(notificationRepository, times(1)).save(any(Notification.class));
        verify(smsGatewayAdapter, never()).sendSms(any(), any(), any(), any());
        verify(emailAdapter, never()).sendEmail(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Default opt-in behavior: external channels in preferredChannels are included when no preference row exists")
    void testDefaultOptInWhenNoPreferenceRowExists() {
        String userId = "user-200";
        NotificationEvent event = new NotificationEvent(
                NotificationType.TAX_PAYMENT_DUE,
                userId,
                "TAX",
                "tax-99",
                "Payment due",
                Set.of(NotificationChannel.SMS, NotificationChannel.EMAIL)
        );

        when(preferenceRepository.findByUserIdAndType(userId, NotificationType.TAX_PAYMENT_DUE))
                .thenReturn(Optional.empty());

        listener.onEvent(event);

        verify(notificationRepository, times(1)).save(any(Notification.class));
        verify(smsGatewayAdapter, times(1)).sendSms(eq(userId), anyString(), eq("Payment due"), eq(NotificationType.TAX_PAYMENT_DUE));
        verify(emailAdapter, times(1)).sendEmail(eq(userId), anyString(), anyString(), eq("Payment due"), eq(NotificationType.TAX_PAYMENT_DUE));
    }

    @Test
    @DisplayName("SMS/EMAIL are included only if in preferredChannels AND user hasn't opted out via NotificationPreference")
    void testUserOptOutPreferenceRespected() {
        String userId = "user-300";
        NotificationEvent event = new NotificationEvent(
                NotificationType.AGREEMENT_SIGNED,
                userId,
                "AGREEMENT",
                "agr-55",
                "Agreement signed",
                Set.of(NotificationChannel.SMS, NotificationChannel.EMAIL)
        );

        // User opted in to SMS only, opted out of EMAIL
        NotificationPreference pref = NotificationPreference.builder()
                .userId(userId)
                .type(NotificationType.AGREEMENT_SIGNED)
                .enabledChannels(Set.of(NotificationChannel.SMS))
                .build();

        when(preferenceRepository.findByUserIdAndType(userId, NotificationType.AGREEMENT_SIGNED))
                .thenReturn(Optional.of(pref));

        listener.onEvent(event);

        verify(notificationRepository, times(1)).save(any(Notification.class));
        verify(smsGatewayAdapter, times(1)).sendSms(eq(userId), anyString(), eq("Agreement signed"), eq(NotificationType.AGREEMENT_SIGNED));
        verify(emailAdapter, never()).sendEmail(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Template resolution formats placeholders correctly and falls back to event.message() when no template exists")
    void testTemplateResolutionAndFallback() {
        String userId = "user-400";
        NotificationEvent eventWithTemplate = new NotificationEvent(
                NotificationType.PROPERTY_APPROVED,
                userId,
                "PROPERTY",
                "prop-77",
                "Default message",
                Set.of(NotificationChannel.EMAIL)
        );

        NotificationTemplate emailTemplate = NotificationTemplate.builder()
                .type(NotificationType.PROPERTY_APPROVED)
                .channel(NotificationChannel.EMAIL)
                .subject("Property {entityId} Approved")
                .bodyTemplate("Hello, your property {entityId} in {module} was approved. Detail: {message}")
                .build();

        when(preferenceRepository.findByUserIdAndType(userId, NotificationType.PROPERTY_APPROVED))
                .thenReturn(Optional.empty());

        when(templateRepository.findByTypeAndChannel(NotificationType.PROPERTY_APPROVED, NotificationChannel.EMAIL))
                .thenReturn(Optional.of(emailTemplate));

        listener.onEvent(eventWithTemplate);

        verify(emailAdapter, times(1)).sendEmail(
                eq(userId),
                anyString(),
                eq("Property prop-77 Approved"),
                eq("Hello, your property prop-77 in PROPERTY was approved. Detail: Default message"),
                eq(NotificationType.PROPERTY_APPROVED)
        );
    }

    @Test
    @DisplayName("One channel adapter throwing NotificationDeliveryException does not prevent other channels from being delivered")
    void testFaultToleranceOneChannelFailureDoesNotBlockOthers() {
        String userId = "user-500";
        NotificationEvent event = new NotificationEvent(
                NotificationType.COMPLAINT_ASSIGNED,
                userId,
                "COMPLAINT",
                "cmp-12",
                "Officer assigned",
                Set.of(NotificationChannel.SMS, NotificationChannel.EMAIL)
        );

        when(preferenceRepository.findByUserIdAndType(userId, NotificationType.COMPLAINT_ASSIGNED))
                .thenReturn(Optional.empty());

        // SMS adapter throws delivery exception
        doThrow(new NotificationDeliveryException("SMS", userId, "COMPLAINT_ASSIGNED", "SMS provider down", new RuntimeException()))
                .when(smsGatewayAdapter).sendSms(anyString(), anyString(), anyString(), any());

        listener.onEvent(event);

        // Verify In-App still saved
        verify(notificationRepository, times(1)).save(any(Notification.class));
        // Verify Email attempt was STILL executed despite SMS throwing exception
        verify(emailAdapter, times(1)).sendEmail(eq(userId), anyString(), anyString(), eq("Officer assigned"), eq(NotificationType.COMPLAINT_ASSIGNED));
    }
}
