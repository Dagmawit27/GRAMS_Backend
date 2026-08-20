package com.ethiorental.backend.notification.dto.request;

import com.ethiorental.backend.shared.notification.NotificationChannel;
import com.ethiorental.backend.shared.notification.NotificationType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreferenceRequest {
    
    @NotNull(message = "Notification type is required")
    private NotificationType type;

    @NotNull(message = "Enabled channels set is required")
    private Set<NotificationChannel> enabledChannels;
}
