package com.ethiorental.backend.notification.service;

import com.ethiorental.backend.notification.exception.NotificationDeliveryException;
import com.ethiorental.backend.shared.notification.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmsGatewayAdapter {

    private static final int MAX_ATTEMPTS = 3;
    private static final long BACKOFF_MS = 500L;

    private final SmsGatewayClient smsGatewayClient;

    /**
     * Sends SMS notification to recipient with automatic retry mechanism.
     * Throws NotificationDeliveryException after max attempts are exhausted.
     */
    public void sendSms(String recipientUserId, String phoneNumber, String message, NotificationType type) {
        int attempt = 0;
        Exception lastException = null;

        while (attempt < MAX_ATTEMPTS) {
            attempt++;
            try {
                log.info("Sending SMS (Attempt {}/{}) for type [{}] to user [{}] phone [{}]",
                        attempt, MAX_ATTEMPTS, type, recipientUserId, phoneNumber);
                
                smsGatewayClient.sendSms(phoneNumber, message, type);
                
                log.info("SMS delivered successfully to [{}] for event [{}]", recipientUserId, type);
                return;
            } catch (Exception ex) {
                lastException = ex;
                log.warn("SMS delivery attempt {} failed for user [{}] event [{}] phone [{}]: {}",
                        attempt, recipientUserId, type, phoneNumber, ex.getMessage());
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
                "SMS",
                recipientUserId,
                type != null ? type.name() : "UNKNOWN",
                String.format("SMS delivery failed after %d attempts for recipient %s (phone %s)", MAX_ATTEMPTS, recipientUserId, phoneNumber),
                lastException
        );
    }
}
