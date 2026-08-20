package com.ethiorental.backend.notification.service;

import com.ethiorental.backend.shared.notification.NotificationType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LoggingEmailClient implements EmailClient {

    @Override
    public void sendEmail(String toEmail, String subject, String body, NotificationType type) {
        log.info("[STUB EMAIL CLIENT] Outgoing Email -> To: [{}], Subject: [{}], Type: [{}], Body: [{}]",
                toEmail, subject, type, body);
    }
}
