package com.ethiorental.backend.notification.exception;

/**
 * Thrown by channel adapters (SmsGatewayAdapter, EmailAdapter) after all retry
 * attempts have been exhausted. Caught — never propagated — by NotificationListener.
 */
public class NotificationDeliveryException extends RuntimeException {

    private final String channel;
    private final String recipientUserId;
    private final String notificationType;

    public NotificationDeliveryException(String channel, String recipientUserId,
                                         String notificationType, String message, Throwable cause) {
        super(message, cause);
        this.channel = channel;
        this.recipientUserId = recipientUserId;
        this.notificationType = notificationType;
    }

    public String getChannel() { return channel; }
    public String getRecipientUserId() { return recipientUserId; }
    public String getNotificationType() { return notificationType; }
}
