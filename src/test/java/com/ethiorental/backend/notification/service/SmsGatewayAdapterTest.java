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

class SmsGatewayAdapterTest {

    private SmsGatewayClient smsGatewayClient;
    private SmsGatewayAdapter adapter;

    @BeforeEach
    void setUp() {
        smsGatewayClient = mock(SmsGatewayClient.class);
        adapter = new SmsGatewayAdapter(smsGatewayClient);
    }

    @Test
    @DisplayName("sendSms succeeds without exception on first attempt")
    void testSendSmsSuccessFirstAttempt() throws Exception {
        assertDoesNotThrow(() ->
                adapter.sendSms("user-1", "+251911000000", "Hello OTP 1234", NotificationType.OTP)
        );

        verify(smsGatewayClient, times(1)).sendSms(eq("+251911000000"), eq("Hello OTP 1234"), eq(NotificationType.OTP));
    }

    @Test
    @DisplayName("sendSms retries and succeeds if second attempt is successful")
    void testSendSmsSuccessSecondAttempt() throws Exception {
        doThrow(new RuntimeException("Temporary Gateway Error"))
                .doNothing()
                .when(smsGatewayClient).sendSms(any(), any(), any());

        assertDoesNotThrow(() ->
                adapter.sendSms("user-1", "+251911000000", "Hello OTP 1234", NotificationType.OTP)
        );

        verify(smsGatewayClient, times(2)).sendSms(eq("+251911000000"), eq("Hello OTP 1234"), eq(NotificationType.OTP));
    }

    @Test
    @DisplayName("sendSms retries exactly 3 times and throws NotificationDeliveryException after exhausting retries")
    void testSendSmsRetryFailureExhausted() throws Exception {
        doThrow(new RuntimeException("SMS Gateway Timeout"))
                .when(smsGatewayClient).sendSms(any(), any(), any());

        assertThrows(NotificationDeliveryException.class, () ->
                adapter.sendSms("user-1", "+251911000000", "Hello", NotificationType.OTP)
        );

        verify(smsGatewayClient, times(3)).sendSms(eq("+251911000000"), eq("Hello"), eq(NotificationType.OTP));
    }
}
