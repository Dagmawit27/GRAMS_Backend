package com.ethiorental.backend.notification.service;

import com.ethiorental.backend.notification.dto.request.NotificationPreferenceRequest;
import com.ethiorental.backend.notification.dto.response.NotificationResponse;
import com.ethiorental.backend.notification.entity.Notification;
import com.ethiorental.backend.notification.entity.NotificationPreference;
import com.ethiorental.backend.notification.repository.NotificationPreferenceRepository;
import com.ethiorental.backend.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationPreferenceRepository preferenceRepository;

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getUserNotifications(String userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Notification> notifications = notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(userId, pageable);
        return notifications.map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(String userId) {
        return notificationRepository.countByRecipientUserIdAndReadFalse(userId);
    }

    @Transactional
    public boolean markAsRead(UUID id, String userId) {
        int updated = notificationRepository.markAsRead(id, userId);
        return updated > 0;
    }

    @Transactional
    public void markAllAsRead(String userId) {
        notificationRepository.markAllAsRead(userId);
    }

    @Transactional
    public NotificationPreference updatePreference(String userId, NotificationPreferenceRequest request) {
        NotificationPreference pref = preferenceRepository.findByUserIdAndType(userId, request.getType())
                .orElseGet(() -> NotificationPreference.builder()
                        .userId(userId)
                        .type(request.getType())
                        .enabledChannels(new HashSet<>())
                        .build());

        pref.updateEnabledChannels(request.getEnabledChannels());
        return preferenceRepository.save(pref);
    }

    private NotificationResponse mapToResponse(Notification n) {
        return toNotificationResponse(n);
    }

    public NotificationResponse toNotificationResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .recipientUserId(n.getRecipientUserId())
                .type(n.getType())
                .module(n.getModule())
                .entityId(n.getEntityId())
                .message(n.getMessage())
                .channel(n.getChannel())
                .read(n.isRead())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
