package com.ethiorental.backend.notification.dto.response;

import com.ethiorental.backend.shared.notification.NotificationChannel;
import com.ethiorental.backend.shared.notification.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {
    private UUID id;
    private String recipientUserId;
    private NotificationType type;
    private String module;
    private String entityId;
    private String message;
    private NotificationChannel channel;
    private boolean read;
    private Instant createdAt;
}
