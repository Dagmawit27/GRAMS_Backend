package com.ethiorental.backend.payment.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "chapa")
public class ChapaProperties {

    /**
     * Chapa API secret key (e.g. CHASECK_TEST-...)
     */
    private String secretKey = "CHASECK_TEST-demo";

    /**
     * Chapa API base URL (defaults to https://api.chapa.co/v1)
     */
    private String baseUrl = "https://api.chapa.co/v1";

    /**
     * Webhook secret for HMAC signature verification
     */
    private String webhookSecret = "demo_webhook_secret";

    /**
     * Default redirect URL for tenant upon checkout completion
     */
    private String returnUrl = "http://localhost:3000/citizen/dashboard/payments";

    /**
     * Server callback webhook URL
     */
    private String callbackUrl = "http://localhost:8080/api/v1/payments/webhook";

    /**
     * Returns true if running in mock/demo sandbox mode
     */
    public boolean isDemoMode() {
        return secretKey == null || secretKey.isBlank() || secretKey.contains("demo");
    }
}
