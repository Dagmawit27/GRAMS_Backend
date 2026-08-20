package com.ethiorental.backend.notification.service;

import com.ethiorental.backend.shared.notification.NotificationEvent;

public interface NotificationListener {
    void onEvent(NotificationEvent event);
}
