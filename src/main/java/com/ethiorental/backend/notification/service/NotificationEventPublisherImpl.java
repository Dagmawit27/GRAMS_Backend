package com.ethiorental.backend.notification.service;

import com.ethiorental.backend.shared.notification.NotificationEvent;
import com.ethiorental.backend.shared.notification.NotificationEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationEventPublisherImpl implements NotificationEventPublisher {

    private final NotificationDispatcher notificationDispatcher;

    @Override
    public void publish(NotificationEvent event) {
        notificationDispatcher.dispatch(event);
    }
}
