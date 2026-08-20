package com.ethiorental.backend.notification.controller;

import com.ethiorental.backend.notification.dto.request.NotificationPreferenceRequest;
import com.ethiorental.backend.notification.dto.response.NotificationResponse;
import com.ethiorental.backend.notification.entity.NotificationPreference;
import com.ethiorental.backend.notification.service.NotificationService;
import com.ethiorental.backend.shared.notification.NotificationChannel;
import com.ethiorental.backend.shared.notification.NotificationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NotificationControllerTest {

    private NotificationService notificationService;
    private NotificationController controller;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        notificationService = mock(NotificationService.class);
        controller = new NotificationController(notificationService);
        userDetails = new User("user-123", "password", Collections.emptyList());
    }

    @Test
    @DisplayName("getNotifications scopes query to authenticated user ID")
    void testGetNotificationsUserScoped() {
        NotificationResponse responseItem = NotificationResponse.builder()
                .id(UUID.randomUUID())
                .recipientUserId("user-123")
                .type(NotificationType.PROPERTY_SUBMITTED)
                .module("PROPERTY")
                .message("Submitted")
                .channel(NotificationChannel.IN_APP)
                .read(false)
                .createdAt(Instant.now())
                .build();

        Page<NotificationResponse> mockPage = new PageImpl<>(List.of(responseItem));
        when(notificationService.getUserNotifications("user-123", 0, 20)).thenReturn(mockPage);

        ResponseEntity<Page<NotificationResponse>> response = controller.getNotifications(userDetails, 0, 20);

        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getTotalElements());
        verify(notificationService).getUserNotifications("user-123", 0, 20);
    }

    @Test
    @DisplayName("getUnreadCount returns unreadCount for authenticated user")
    void testGetUnreadCount() {
        when(notificationService.getUnreadCount("user-123")).thenReturn(5L);

        ResponseEntity<Map<String, Long>> response = controller.getUnreadCount(userDetails);

        assertNotNull(response.getBody());
        assertEquals(5L, response.getBody().get("unreadCount"));
        verify(notificationService).getUnreadCount("user-123");
    }

    @Test
    @DisplayName("markAsRead returns 200 when owned by user, 404 when not found or owned by another user")
    void testMarkAsReadUserScoped() {
        UUID notifId = UUID.randomUUID();
        when(notificationService.markAsRead(notifId, "user-123")).thenReturn(true);

        ResponseEntity<Void> response = controller.markAsRead(userDetails, notifId);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        when(notificationService.markAsRead(notifId, "user-123")).thenReturn(false);
        ResponseEntity<Void> notFoundResp = controller.markAsRead(userDetails, notifId);
        assertEquals(HttpStatus.NOT_FOUND, notFoundResp.getStatusCode());
    }

    @Test
    @DisplayName("markAllAsRead delegates with authenticated user ID")
    void testMarkAllAsReadUserScoped() {
        ResponseEntity<Void> response = controller.markAllAsRead(userDetails);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(notificationService).markAllAsRead("user-123");
    }

    @Test
    @DisplayName("updatePreferences delegates to service with authenticated user ID")
    void testUpdatePreferencesUserScoped() {
        NotificationPreferenceRequest request = NotificationPreferenceRequest.builder()
                .type(NotificationType.TAX_PAYMENT_DUE)
                .enabledChannels(Set.of(NotificationChannel.SMS))
                .build();

        NotificationPreference preference = NotificationPreference.builder()
                .userId("user-123")
                .type(NotificationType.TAX_PAYMENT_DUE)
                .enabledChannels(Set.of(NotificationChannel.SMS))
                .build();

        when(notificationService.updatePreference("user-123", request)).thenReturn(preference);

        ResponseEntity<NotificationPreference> response = controller.updatePreferences(userDetails, request);

        assertNotNull(response.getBody());
        assertEquals("user-123", response.getBody().getUserId());
        verify(notificationService).updatePreference("user-123", request);
    }

    @Test
    @DisplayName("Rejects request when userDetails is null (unauthenticated)")
    void testUnauthenticatedRequestRejected() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                controller.getNotifications(null, 0, 20)
        );
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }
}
