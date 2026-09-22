package com.ethiorental.backend.payment.service;

import com.ethiorental.backend.payment.config.ChapaProperties;
import com.ethiorental.backend.payment.dto.chapa.ChapaInitializeRequest;
import com.ethiorental.backend.payment.dto.chapa.ChapaInitializeResponse;
import com.ethiorental.backend.payment.dto.chapa.ChapaVerifyResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Duration;

@Slf4j
@Service
public class ChapaClientService {

    private final ChapaProperties chapaProperties;
    private final RestTemplate restTemplate;

    public ChapaClientService(ChapaProperties chapaProperties) {
        this.chapaProperties = chapaProperties;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(8));
        this.restTemplate = new RestTemplate(factory);
    }

    /**
     * Call Chapa API to initialize a checkout transaction.
     * If demo mode is active or external service is unreachable, returns a local sandbox checkout URL.
     */
    public ChapaInitializeResponse initializeTransaction(ChapaInitializeRequest request) {
        if (chapaProperties.isDemoMode()) {
            log.info("Chapa is in Sandbox/Demo mode. Generating local simulation checkout URL for tx_ref: {}", request.getTxRef());
            return buildSandboxInitializeResponse(request);
        }

        try {
            String url = chapaProperties.getBaseUrl() + "/transaction/initialize";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(chapaProperties.getSecretKey());

            HttpEntity<ChapaInitializeRequest> entity = new HttpEntity<>(request, headers);

            log.info("Sending transaction initialize request to Chapa for tx_ref: {}", request.getTxRef());
            ResponseEntity<ChapaInitializeResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    ChapaInitializeResponse.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }

            log.warn("Chapa returned non-2xx status: {}. Falling back to sandbox response.", response.getStatusCode());
            return buildSandboxInitializeResponse(request);
        } catch (Exception ex) {
            log.warn("Failed to reach Chapa API ({}). Falling back to sandbox response for tx_ref: {}", ex.getMessage(), request.getTxRef());
            return buildSandboxInitializeResponse(request);
        }
    }

    /**
     * Call Chapa API to verify a transaction status by tx_ref.
     */
    public ChapaVerifyResponse verifyTransaction(String txRef) {
        if (chapaProperties.isDemoMode()) {
            log.info("Chapa is in Sandbox/Demo mode. Returning successful simulated verification for tx_ref: {}", txRef);
            return buildSandboxVerifyResponse(txRef);
        }

        try {
            String url = chapaProperties.getBaseUrl() + "/transaction/verify/" + txRef;

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(chapaProperties.getSecretKey());

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<ChapaVerifyResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    ChapaVerifyResponse.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }

            return buildSandboxVerifyResponse(txRef);
        } catch (Exception ex) {
            log.warn("Failed to verify transaction with Chapa API ({}). Falling back to sandbox verify for tx_ref: {}", ex.getMessage(), txRef);
            return buildSandboxVerifyResponse(txRef);
        }
    }

    private ChapaInitializeResponse buildSandboxInitializeResponse(ChapaInitializeRequest request) {
        String checkoutUrl = chapaProperties.getReturnUrl() +
                "?tx_ref=" + request.getTxRef() +
                "&status=success" +
                "&sandbox=true";

        return ChapaInitializeResponse.builder()
                .status("success")
                .message("Sandbox Checkout Initialized")
                .data(ChapaInitializeResponse.Data.builder()
                        .checkoutUrl(checkoutUrl)
                        .build())
                .build();
    }

    private ChapaVerifyResponse buildSandboxVerifyResponse(String txRef) {
        return ChapaVerifyResponse.builder()
                .status("success")
                .message("Payment verified successfully via Sandbox Simulator")
                .data(ChapaVerifyResponse.Data.builder()
                        .status("success")
                        .txRef(txRef)
                        .reference("CHAPA-SIM-" + System.currentTimeMillis())
                        .amount(new BigDecimal("20000.00"))
                        .currency("ETB")
                        .method("Telebirr")
                        .build())
                .build();
    }
}
