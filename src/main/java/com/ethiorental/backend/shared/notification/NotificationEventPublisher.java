package com.ethiorental.backend.shared.notification;

public interface NotificationEventPublisher {
    void publish(NotificationEvent event);
}