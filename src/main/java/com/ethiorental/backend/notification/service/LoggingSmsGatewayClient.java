package com.ethiorental.backend.notification.service;

import com.ethiorental.backend.shared.notification.NotificationType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LoggingSmsGatewayClient implements SmsGatewayClient {

    @Override
    public void sendSms(String phoneNumber, String message, NotificationType type) {
        log.info("[STUB SMS GATEWAY] Outgoing SMS -> Phone: [{}], Type: [{}], Message: [{}]",
                phoneNumber, type, message);
    }
}
