package com.ethiorental.backend.shared.notification;

import java.util.Set;

public record NotificationEvent(
    NotificationType type,
    String recipientUserId,
    String module,          // "PROPERTY", "AGREEMENT", "TAX", "COMPLAINT"
    String entityId,
    String message,         // fallback/default message; templates can override
    Set<NotificationChannel> preferredChannels // publisher's suggestion; NotificationPreference may override
) {}