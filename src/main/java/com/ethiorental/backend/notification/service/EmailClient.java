package com.ethiorental.backend.notification.service;

import com.ethiorental.backend.shared.notification.NotificationType;

public interface EmailClient {
    void sendEmail(String toEmail, String subject, String body, NotificationType type) throws Exception;
}
