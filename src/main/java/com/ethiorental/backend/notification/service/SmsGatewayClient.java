package com.ethiorental.backend.notification.service;

import com.ethiorental.backend.shared.notification.NotificationType;

public interface SmsGatewayClient {
    void sendSms(String phoneNumber, String message, NotificationType type) throws Exception;
}
