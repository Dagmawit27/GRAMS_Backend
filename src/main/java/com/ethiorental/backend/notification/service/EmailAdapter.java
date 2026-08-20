package com.ethiorental.backend.notification.service;

import com.ethiorental.backend.notification.exception.NotificationDeliveryException;
import com.ethiorental.backend.shared.notification.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailAdapter {

    private static final int MAX_ATTEMPTS = 3;
    private static final long BACKOFF_MS = 500L;

    private final EmailClient emailClient;

    /**
     * Sends Email notification to recipient with automatic retry mechanism.
     * Throws NotificationDeliveryException after max attempts are exhausted.
     */
    public void sendEmail(String recipientUserId, String emailAddress, String subject, String body, NotificationType type) {
        int attempt = 0;
        Exception lastException = null;

        while (attempt < MAX_ATTEMPTS) {
            attempt++;
            try {
                log.info("Sending Email (Attempt {}/{}) for type [{}] to user [{}] email [{}]",
                        attempt, MAX_ATTEMPTS, type, recipientUserId, emailAddress);
                
                emailClient.sendEmail(emailAddress, subject, body, type);
                
                log.info("Email delivered successfully to [{}] for event [{}]", recipientUserId, type);
                return;
            } catch (Exception ex) {
                lastException = ex;
                log.warn("Email delivery attempt {} failed for user [{}] event [{}] email [{}]: {}",
                        attempt, recipientUserId, type, emailAddress, ex.getMessage());
                if (attempt < MAX_ATTEMPTS) {
                    try {
                        Thread.sleep(BACKOFF_MS * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        throw new NotificationDeliveryException(
                "EMAIL",
                recipientUserId,
                type != null ? type.name() : "UNKNOWN",
                String.format("Email delivery failed after %d attempts for recipient %s (email %s)", MAX_ATTEMPTS, recipientUserId, emailAddress),
                lastException
        );
    }
}
