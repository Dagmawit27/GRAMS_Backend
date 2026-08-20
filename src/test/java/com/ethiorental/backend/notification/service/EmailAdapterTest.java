package com.ethiorental.backend.notification.service;

import com.ethiorental.backend.notification.exception.NotificationDeliveryException;
import com.ethiorental.backend.shared.notification.NotificationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class EmailAdapterTest {

    private EmailClient emailClient;
    private EmailAdapter adapter;

    @BeforeEach
    void setUp() {
        emailClient = mock(EmailClient.class);
        adapter = new EmailAdapter(emailClient);
    }

    @Test
    @DisplayName("sendEmail succeeds without exception on first attempt")
    void testSendEmailSuccessFirstAttempt() throws Exception {
        assertDoesNotThrow(() ->
                adapter.sendEmail("user-1", "test@example.com", "Subject", "Body content", NotificationType.ACCOUNT_VERIFICATION)
        );

        verify(emailClient, times(1)).sendEmail(eq("test@example.com"), eq("Subject"), eq("Body content"), eq(NotificationType.ACCOUNT_VERIFICATION));
    }

    @Test
    @DisplayName("sendEmail retries and succeeds if second attempt is successful")
    void testSendEmailSuccessSecondAttempt() throws Exception {
        doThrow(new RuntimeException("Transient SMTP Error"))
                .doNothing()
                .when(emailClient).sendEmail(any(), any(), any(), any());

        assertDoesNotThrow(() ->
                adapter.sendEmail("user-1", "test@example.com", "Subject", "Body content", NotificationType.ACCOUNT_VERIFICATION)
        );

        verify(emailClient, times(2)).sendEmail(eq("test@example.com"), eq("Subject"), eq("Body content"), eq(NotificationType.ACCOUNT_VERIFICATION));
    }

    @Test
    @DisplayName("sendEmail retries exactly 3 times and throws NotificationDeliveryException after exhausting retries")
    void testSendEmailRetryFailureExhausted() throws Exception {
        doThrow(new RuntimeException("SMTP Connection Refused"))
                .when(emailClient).sendEmail(any(), any(), any(), any());

        assertThrows(NotificationDeliveryException.class, () ->
                adapter.sendEmail("user-1", "test@example.com", "Subject", "Body", NotificationType.ACCOUNT_VERIFICATION)
        );

        verify(emailClient, times(3)).sendEmail(eq("test@example.com"), eq("Subject"), eq("Body"), eq(NotificationType.ACCOUNT_VERIFICATION));
    }
}
