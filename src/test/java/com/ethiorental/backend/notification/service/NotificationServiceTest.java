package com.ethiorental.backend.notification.service;

import com.ethiorental.backend.notification.dto.request.NotificationPreferenceRequest;
import com.ethiorental.backend.notification.dto.response.NotificationResponse;
import com.ethiorental.backend.notification.entity.Notification;
import com.ethiorental.backend.notification.entity.NotificationPreference;
import com.ethiorental.backend.notification.repository.NotificationPreferenceRepository;
import com.ethiorental.backend.notification.repository.NotificationRepository;
import com.ethiorental.backend.shared.notification.NotificationChannel;
import com.ethiorental.backend.shared.notification.NotificationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class NotificationServiceTest {

    private NotificationRepository notificationRepository;
    private NotificationPreferenceRepository preferenceRepository;
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationRepository = mock(NotificationRepository.class);
        preferenceRepository = mock(NotificationPreferenceRepository.class);
        notificationService = new NotificationService(notificationRepository, preferenceRepository);
    }

    @Test
    @DisplayName("getUserNotifications delegates to findByRecipientUserIdOrderByCreatedAtDesc with correct pageable")
    void testGetUserNotificationsPaginatedAndOrdered() {
        String userId = "user-abc";
        Notification notification = Notification.builder()
                .id(UUID.randomUUID())
                .recipientUserId(userId)
                .type(NotificationType.PROPERTY_APPROVED)
                .module("PROPERTY")
                .message("Property approved")
                .channel(NotificationChannel.IN_APP)
                .read(false)
                .createdAt(Instant.now())
                .build();

        Page<Notification> mockPage = new PageImpl<>(List.of(notification));
        when(notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(eq(userId), eq(PageRequest.of(0, 10))))
                .thenReturn(mockPage);

        Page<NotificationResponse> result = notificationService.getUserNotifications(userId, 0, 10);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("user-abc", result.getContent().get(0).getRecipientUserId());
        verify(notificationRepository).findByRecipientUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 10));
    }

    @Test
    @DisplayName("markAsRead returns true when notification belongs to requesting user, false otherwise")
    void testMarkAsReadUserScoping() {
        UUID notifId = UUID.randomUUID();
        String requestingUser = "user-owner";
        String wrongUser = "user-attacker";

        when(notificationRepository.markAsRead(notifId, requestingUser)).thenReturn(1);
        when(notificationRepository.markAsRead(notifId, wrongUser)).thenReturn(0);

        boolean success = notificationService.markAsRead(notifId, requestingUser);
        boolean denied = notificationService.markAsRead(notifId, wrongUser);

        assertTrue(success, "Should succeed when notification belongs to user");
        assertFalse(denied, "Should return false/deny when notification does not belong to requesting user");
        verify(notificationRepository).markAsRead(notifId, requestingUser);
        verify(notificationRepository).markAsRead(notifId, wrongUser);
    }

    @Test
    @DisplayName("markAllAsRead only targets the requesting user's notifications")
    void testMarkAllAsReadUserScoped() {
        String userId = "user-123";

        notificationService.markAllAsRead(userId);

        verify(notificationRepository, times(1)).markAllAsRead(userId);
    }

    @Test
    @DisplayName("updatePreference creates or mutates existing preference row correctly")
    void testUpdatePreferenceMutatesExisting() {
        String userId = "user-456";
        NotificationType type = NotificationType.TAX_PAYMENT_DUE;
        
        NotificationPreference existingPref = NotificationPreference.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .type(type)
                .enabledChannels(Set.of(NotificationChannel.IN_APP))
                .build();

        when(preferenceRepository.findByUserIdAndType(userId, type))
                .thenReturn(Optional.of(existingPref));

        NotificationPreferenceRequest request = NotificationPreferenceRequest.builder()
                .type(type)
                .enabledChannels(Set.of(NotificationChannel.SMS, NotificationChannel.EMAIL))
                .build();

        when(preferenceRepository.save(any(NotificationPreference.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        NotificationPreference result = notificationService.updatePreference(userId, request);

        assertNotNull(result);
        assertEquals(2, result.getEnabledChannels().size());
        assertTrue(result.getEnabledChannels().contains(NotificationChannel.SMS));
        assertTrue(result.getEnabledChannels().contains(NotificationChannel.EMAIL));

        ArgumentCaptor<NotificationPreference> captor = ArgumentCaptor.forClass(NotificationPreference.class);
        verify(preferenceRepository).save(captor.capture());
        assertEquals(existingPref.getId(), captor.getValue().getId(), "Must mutate existing entity row");
    }
}
