package com.ethiorental.backend.payment.service;

import com.ethiorental.backend.IAM.entity.Citizen;
import com.ethiorental.backend.IAM.repository.CitizenRepository;
import com.ethiorental.backend.agreement.entity.Agreement;
import com.ethiorental.backend.agreement.enums.AgreementStatus;
import com.ethiorental.backend.agreement.repository.AgreementRepository;
import com.ethiorental.backend.notification.controller.NotificationSseController;
import com.ethiorental.backend.notification.dto.response.NotificationResponse;
import com.ethiorental.backend.notification.entity.Notification;
import com.ethiorental.backend.notification.repository.NotificationRepository;
import com.ethiorental.backend.notification.service.NotificationService;
import com.ethiorental.backend.payment.config.ChapaProperties;
import com.ethiorental.backend.payment.dto.chapa.ChapaInitializeRequest;
import com.ethiorental.backend.payment.dto.chapa.ChapaInitializeResponse;
import com.ethiorental.backend.payment.dto.chapa.ChapaVerifyResponse;
import com.ethiorental.backend.payment.dto.request.PaymentInitiateRequest;
import com.ethiorental.backend.payment.dto.response.PaymentInitiateResponse;
import com.ethiorental.backend.payment.dto.response.PaymentResponse;
import com.ethiorental.backend.payment.entity.Payment;
import com.ethiorental.backend.payment.enums.PaymentStatus;
import com.ethiorental.backend.payment.repository.PaymentRepository;
import com.ethiorental.backend.shared.notification.NotificationChannel;
import com.ethiorental.backend.shared.notification.NotificationType;
import com.ethiorental.backend.tax.service.TaxService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final AgreementRepository agreementRepository;
    private final CitizenRepository citizenRepository;
    private final ChapaClientService chapaClientService;
    private final ChapaProperties chapaProperties;
    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;
    private final NotificationSseController sseController;
    private final TaxService taxService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional
    public PaymentInitiateResponse initiateAdvancePayment(String username, PaymentInitiateRequest request) {
        // Normalize and resolve agreement by requestCode, agreementNumber, or UUID
        String rawCode = request.getRequestCode() != null ? request.getRequestCode().trim() : "";
        String cleanCode = rawCode;
        if (cleanCode.startsWith("agr-inv-")) {
            cleanCode = cleanCode.substring("agr-inv-".length());
        }
        if (cleanCode.startsWith("INV-")) {
            cleanCode = cleanCode.substring("INV-".length());
        }

        final String lookupKey = cleanCode;
        Optional<Agreement> agreementOpt = agreementRepository.findByRequestCode(lookupKey)
                .or(() -> agreementRepository.findByAgreementNumber(lookupKey))
                .or(() -> agreementRepository.findByRequestCode(rawCode))
                .or(() -> agreementRepository.findByAgreementNumber(rawCode));

        if (agreementOpt.isEmpty()) {
            try {
                UUID uuid = UUID.fromString(lookupKey);
                agreementOpt = agreementRepository.findById(uuid);
            } catch (Exception ignored) {}
        }

        if (agreementOpt.isEmpty()) {
            // Comprehensive scan fallback
            List<Agreement> all = agreementRepository.findAll();
            for (Agreement a : all) {
                if (lookupKey.equalsIgnoreCase(a.getRequestCode()) ||
                        lookupKey.equalsIgnoreCase(a.getAgreementNumber()) ||
                        (a.getId() != null && lookupKey.equalsIgnoreCase(a.getId().toString())) ||
                        rawCode.equalsIgnoreCase(a.getRequestCode()) ||
                        rawCode.equalsIgnoreCase(a.getAgreementNumber())) {
                    agreementOpt = Optional.of(a);
                    break;
                }
            }
        }

        Agreement agreement = agreementOpt.orElseThrow(() ->
                new IllegalArgumentException("Agreement not found for identifier: " + request.getRequestCode()));

        Citizen tenant = agreement.getTenant();
        Citizen landlord = agreement.getLandlord();

        // Enforce landlord payout configuration
        if (landlord.getAccountNumber() == null || landlord.getAccountNumber().trim().isEmpty()) {
            throw new IllegalStateException("Landlord (" + landlord.getFirstName() + " " + landlord.getMiddleName() + ") has not configured a payout account yet. Payout accounts must be configured in profile settings before receiving rent.");
        }

        // Calculate amount for advance payment
        int advanceMonths = agreement.getAdvancePaymentMonths() != null && agreement.getAdvancePaymentMonths() > 0
                ? agreement.getAdvancePaymentMonths()
                : 1;

        BigDecimal totalAmount = request.getAmount() != null && request.getAmount().compareTo(BigDecimal.ZERO) > 0
                ? request.getAmount()
                : agreement.getMonthlyRent().multiply(BigDecimal.valueOf(advanceMonths));

        String txRef = "TX-" + agreement.getRequestCode() + "-" + System.currentTimeMillis();

        String tenantFullName = ((tenant.getFirstName() != null ? tenant.getFirstName() : "") +
                (tenant.getLastName() != null ? " " + tenant.getLastName() : "")).trim();
        String landlordFullName = ((landlord.getFirstName() != null ? landlord.getFirstName() : "") +
                (landlord.getLastName() != null ? " " + landlord.getLastName() : "")).trim();

        // Determine chosen destination account (from tenant request or landlord primary)
        String destBank = (request.getDestinationBankName() != null && !request.getDestinationBankName().isBlank())
                ? request.getDestinationBankName()
                : landlord.getBankName();
        String destAccount = (request.getDestinationAccountNumber() != null && !request.getDestinationAccountNumber().isBlank())
                ? request.getDestinationAccountNumber()
                : landlord.getAccountNumber();
        String destHolder = (request.getDestinationAccountHolderName() != null && !request.getDestinationAccountHolderName().isBlank())
                ? request.getDestinationAccountHolderName()
                : (landlord.getAccountHolderName() != null ? landlord.getAccountHolderName() : landlordFullName);

        // Build Chapa initialize request
        Map<String, Object> customization = new HashMap<>();
        customization.put("title", "Addis Rental Advance Payment");
        customization.put("description", advanceMonths + " Months Advance Rent for " +
                (agreement.getProperty() != null ? agreement.getProperty().getPropertyCode() : "Property"));

        Map<String, Object> meta = new HashMap<>();
        meta.put("agreementId", agreement.getId().toString());
        meta.put("agreementNumber", agreement.getAgreementNumber());
        meta.put("requestCode", agreement.getRequestCode());
        meta.put("landlordId", landlord.getId().toString());
        meta.put("landlordBank", destBank);
        meta.put("landlordAccountNumber", destAccount);
        meta.put("advanceMonths", String.valueOf(advanceMonths));

        String returnUrl = chapaProperties.getReturnUrl() + "?tx_ref=" + txRef;

        ChapaInitializeRequest chapaReq = ChapaInitializeRequest.builder()
                .amount(totalAmount.toPlainString())
                .currency("ETB")
                .email(tenant.getEmail())
                .firstName(tenant.getFirstName() != null ? tenant.getFirstName() : "Citizen")
                .lastName(tenant.getLastName() != null ? tenant.getLastName() : "Tenant")
                .phoneNumber(request.getPhoneNumber() != null ? request.getPhoneNumber() : tenant.getPhone())
                .txRef(txRef)
                .callbackUrl(chapaProperties.getCallbackUrl())
                .returnUrl(returnUrl)
                .customization(customization)
                .meta(meta)
                .build();

        ChapaInitializeResponse chapaRes = chapaClientService.initializeTransaction(chapaReq);
        String checkoutUrl = (chapaRes.getData() != null && chapaRes.getData().getCheckoutUrl() != null)
                ? chapaRes.getData().getCheckoutUrl()
                : returnUrl + "&status=success&sandbox=true";

        // Persist Payment record
        Payment payment = Payment.builder()
                .txRef(txRef)
                .agreement(agreement)
                .agreementNumber(agreement.getAgreementNumber())
                .requestCode(agreement.getRequestCode())
                .propertyTitle(agreement.getProperty() != null ? agreement.getProperty().getPropertyCode() : "Residential Lease")
                .tenant(tenant)
                .tenantName(tenantFullName)
                .tenantEmail(tenant.getEmail())
                .landlord(landlord)
                .landlordName(landlordFullName)
                .landlordEmail(landlord.getEmail())
                .landlordBankName(destBank)
                .landlordAccountNumber(destAccount)
                .landlordAccountHolderName(destHolder)
                .amount(totalAmount)
                .taxAmount(taxService.calculatePaymentTaxAllocation(totalAmount, agreement.getMonthlyRent()))
                .netLandlordAmount(totalAmount.subtract(taxService.calculatePaymentTaxAllocation(totalAmount, agreement.getMonthlyRent())))
                .currency("ETB")
                .status(PaymentStatus.PENDING)
                .paymentMethod(request.getPaymentMethod() != null ? request.getPaymentMethod() : "Chapa Gateway")
                .checkoutUrl(checkoutUrl)
                .build();

        paymentRepository.save(payment);
        log.info("Saved pending payment record for txRef: {} with checkoutUrl: {}", txRef, checkoutUrl);

        return PaymentInitiateResponse.builder()
                .txRef(txRef)
                .checkoutUrl(checkoutUrl)
                .amount(totalAmount)
                .currency("ETB")
                .status(PaymentStatus.PENDING.name())
                .requestCode(agreement.getRequestCode())
                .agreementNumber(agreement.getAgreementNumber())
                .propertyTitle(payment.getPropertyTitle())
                .landlordName(landlordFullName)
                .landlordBankName(landlord.getBankName())
                .landlordAccountNumber(landlord.getAccountNumber())
                .landlordAccountHolderName(payment.getLandlordAccountHolderName())
                .sandboxMode(chapaProperties.isDemoMode())
                .build();
    }

    @Override
    @Transactional
    public PaymentResponse verifyAndCompletePayment(String txRef) {
        log.info("Verifying and completing payment for txRef: {}", txRef);

        Payment payment = paymentRepository.findByTxRef(txRef)
                .orElseThrow(() -> new IllegalArgumentException("Payment record not found for txRef: " + txRef));

        if (payment.getStatus() == PaymentStatus.COMPLETED) {
            log.info("Payment {} already marked COMPLETED", txRef);
            return toResponse(payment);
        }

        ChapaVerifyResponse verifyRes = chapaClientService.verifyTransaction(txRef);
        boolean isSuccess = verifyRes != null && "success".equalsIgnoreCase(verifyRes.getStatus()) &&
                verifyRes.getData() != null && "success".equalsIgnoreCase(verifyRes.getData().getStatus());

        if (isSuccess) {
            payment.setStatus(PaymentStatus.COMPLETED);
            payment.setPaymentDate(LocalDateTime.now());
            if (verifyRes.getData().getReference() != null) {
                payment.setChapaReference(verifyRes.getData().getReference());
            } else if (payment.getChapaReference() == null) {
                payment.setChapaReference("CHAPA-SIM-" + System.currentTimeMillis());
            }
            if (verifyRes.getData().getMethod() != null && (payment.getPaymentMethod() == null || payment.getPaymentMethod().isBlank() || "Chapa Gateway".equalsIgnoreCase(payment.getPaymentMethod()))) {
                payment.setPaymentMethod(verifyRes.getData().getMethod());
            }

            // Ensure agreement is marked ACTIVE and advance payment state tracking
            if (payment.getAgreement() != null) {
                Agreement agr = payment.getAgreement();
                agr.setStatus(AgreementStatus.ACTIVE);

                int advMonths = agr.getAdvancePaymentMonths() != null && agr.getAdvancePaymentMonths() > 0
                        ? agr.getAdvancePaymentMonths() : 1;
                LocalDate start = agr.getStartDate() != null
                        ? agr.getStartDate().toLocalDate()
                        : (agr.getContractDate() != null ? agr.getContractDate() : LocalDate.now());

                if (agr.getMonthlyPaymentDueDay() == null) {
                    agr.setMonthlyPaymentDueDay(start.getDayOfMonth());
                }

                int monthsPaid;
                if (agr.getTotalMonthsPaid() == null || agr.getTotalMonthsPaid() < advMonths) {
                    // Initial Advance Rent payment completion
                    monthsPaid = advMonths;
                    agr.setTotalMonthsPaid(advMonths);
                    LocalDate paidThrough = start.plusMonths(advMonths);
                    agr.setPaidThroughDate(paidThrough);
                    // Next payment is due the day after paid-through date
                    LocalDate nextDue = paidThrough.plusDays(1);
                    agr.setNextPaymentDueDate(nextDue);
                    // Period covered starts from agreement start date
                    payment.setPeriodCoveredDate(start.atStartOfDay());
                    log.info("Advance rent ({} months) marked completed for agreement {}. Paid through: {}, Next due: {}, Period covered from: {}",
                            advMonths, agr.getAgreementNumber(), paidThrough, nextDue, start);
                } else {
                    // Recurring monthly rent payment
                    monthsPaid = 1;
                    if (agr.getMonthlyRent() != null && agr.getMonthlyRent().compareTo(BigDecimal.ZERO) > 0 && payment.getAmount() != null) {
                        try {
                            BigDecimal divided = payment.getAmount().divideToIntegralValue(agr.getMonthlyRent());
                            if (divided.intValue() > 0) {
                                monthsPaid = divided.intValue();
                            }
                        } catch (Exception ignored) {}
                    }
                    // Period covered is the nextPaymentDueDate (the due date for this payment)
                    LocalDate periodCovered = agr.getNextPaymentDueDate() != null
                            ? agr.getNextPaymentDueDate()
                            : start.plusMonths(agr.getTotalMonthsPaid());
                    payment.setPeriodCoveredDate(periodCovered.atStartOfDay());

                    int newTotalMonths = agr.getTotalMonthsPaid() + monthsPaid;
                    agr.setTotalMonthsPaid(newTotalMonths);
                    LocalDate baseDate = agr.getPaidThroughDate() != null
                            ? agr.getPaidThroughDate()
                            : start.plusMonths(agr.getTotalMonthsPaid());
                    LocalDate newPaidThrough = baseDate.plusMonths(monthsPaid);
                    agr.setPaidThroughDate(newPaidThrough);
                    // Next payment is due the day after paid-through date
                    LocalDate nextDue = newPaidThrough.plusDays(1);
                    agr.setNextPaymentDueDate(nextDue);
                    log.info("Recurring rent (+{} month(s)) marked completed for agreement {}. Total months paid: {}, Paid through: {}, Next due: {}, Period covered: {}",
                            monthsPaid, agr.getAgreementNumber(), newTotalMonths, newPaidThrough, nextDue, periodCovered);
                }

                agreementRepository.save(agr);

                // Update LandlordTax record in tax module
                taxService.onPaymentCompleted(agr, payment, monthsPaid);
            }

            paymentRepository.save(payment);
            log.info("Payment {} marked COMPLETED. Dispatched to landlord account: {} ({})",
                    txRef, payment.getLandlordAccountNumber(), payment.getLandlordBankName());

            // Emit real-time SSE notifications to Tenant and Landlord
            emitPaymentNotifications(payment);
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            log.warn("Payment verification failed for txRef: {}", txRef);
        }

        return toResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByTxRef(String txRef) {
        return paymentRepository.findByTxRef(txRef)
                .map(this::toResponse)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found for txRef: " + txRef));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsForAgreement(UUID agreementId) {
        return paymentRepository.findByAgreementId(agreementId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsForAgreement(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return Collections.emptyList();
        }

        String clean = identifier.trim();
        if (clean.startsWith("agr-inv-")) clean = clean.substring("agr-inv-".length());
        if (clean.startsWith("INV-")) clean = clean.substring("INV-".length());

        // 1. Try UUID
        try {
            UUID uuid = UUID.fromString(clean);
            List<PaymentResponse> byUuid = getPaymentsForAgreement(uuid);
            if (!byUuid.isEmpty()) return byUuid;
        } catch (Exception ignored) {}

        // 2. Try by requestCode
        List<Payment> byReq = paymentRepository.findByRequestCodeOrderByCreatedAtDesc(clean);
        if (!byReq.isEmpty()) {
            return byReq.stream().map(this::toResponse).collect(Collectors.toList());
        }

        // 3. Try by agreementNumber
        List<Payment> byNum = paymentRepository.findByAgreementNumberOrderByCreatedAtDesc(clean);
        if (!byNum.isEmpty()) {
            return byNum.stream().map(this::toResponse).collect(Collectors.toList());
        }

        // 4. Resolve agreement entity and get payments by its ID
        final String searchKey = clean;
        Optional<Agreement> agreementOpt = agreementRepository.findByRequestCode(searchKey)
                .or(() -> agreementRepository.findByAgreementNumber(searchKey))
                .or(() -> agreementRepository.findByRequestCode(identifier));

        if (agreementOpt.isPresent()) {
            return getPaymentsForAgreement(agreementOpt.get().getId());
        }

        return Collections.emptyList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> getMyPayments(String username) {
        if (username == null || username.isBlank()) {
            return Collections.emptyList();
        }

        Optional<Citizen> citizenOpt = citizenRepository.findByEmail(username)
                .or(() -> citizenRepository.findByPhone(username));

        String email = citizenOpt.map(Citizen::getEmail).orElse(username);
        String phone = citizenOpt.map(Citizen::getPhone).orElse(username);
        List<Payment> tenantPayments = paymentRepository.findByTenantEmail(email);
        List<Payment> landlordPayments = paymentRepository.findByLandlordEmail(email);

        Set<UUID> seen = new HashSet<>();
        List<PaymentResponse> result = new ArrayList<>();

        for (Payment p : tenantPayments) {
            if (seen.add(p.getId())) {
                result.add(toResponse(p));
            }
        }
        for (Payment p : landlordPayments) {
            if (seen.add(p.getId())) {
                result.add(toResponse(p));
            }
        }

        result.sort((a, b) -> {
            if (a.getCreatedAt() != null && b.getCreatedAt() != null) {
                return b.getCreatedAt().compareTo(a.getCreatedAt());
            }
            return 0;
        });

        return result;
    }

    @Override
    @Transactional
    public void handleWebhook(String payload, String signatureHeader) {
        log.info("Received Chapa webhook event. Payload length: {}", payload != null ? payload.length() : 0);

        try {
            if (payload == null || payload.isBlank()) return;

            Map<String, Object> data = objectMapper.readValue(payload, Map.class);
            String txRef = (String) data.get("tx_ref");
            String status = (String) data.get("status");

            if (txRef != null && "success".equalsIgnoreCase(status)) {
                verifyAndCompletePayment(txRef);
            }
        } catch (Exception ex) {
            log.error("Error processing Chapa webhook: {}", ex.getMessage(), ex);
        }
    }

    private void emitPaymentNotifications(Payment payment) {
        try {
            // 1. Notify Tenant (designated as Rent Payment)
            String tenantMsg = String.format("Advance Rent Payment: ETB %s for %s has been settled successfully via Chapa. (TxRef: %s)",
                    payment.getAmount().toPlainString(), payment.getPropertyTitle(), payment.getTxRef());
            createAndSendNotification(payment.getTenantEmail(), NotificationType.RENT_PAYMENT_SETTLED, "PAYMENT",
                    payment.getTxRef(), tenantMsg);

            // 2. Notify Landlord (designated as Rent Payment with Schedule B tax allocation)
            BigDecimal taxAmt = payment.getTaxAmount() != null ? payment.getTaxAmount() : BigDecimal.ZERO;
            if (taxAmt.compareTo(BigDecimal.ZERO) == 0 && payment.getAmount() != null) {
                taxAmt = taxService.calculatePaymentTaxAllocation(payment.getAmount(),
                        payment.getAgreement() != null ? payment.getAgreement().getMonthlyRent() : null);
            }
            int bracketRate = taxService.getScheduleBBracketRate(
                    payment.getAgreement() != null && payment.getAgreement().getMonthlyRent() != null
                            ? payment.getAgreement().getMonthlyRent().multiply(BigDecimal.valueOf(12))
                            : payment.getAmount().multiply(BigDecimal.valueOf(6))
            );

            String landlordMsg = String.format(
                    "Rent Payment Received: Tenant %s has paid ETB %s for %s directly into your %s account (%s). " +
                    "Under Schedule B (የቤት ኪራይ ገቢ ግብር), estimated tax reserve for this payment is ETB %s (%d%% bracket), payable annually in summer (Hamle - Nehase). " +
                    "በኢትዮጵያ ሕግ መሠረት የቤት ኪራይ ገቢ ግብር በ«ሸለቆ B» (Schedule B) ስር የሚመደብ ሲሆን፣ በክረምት (ከሐምሌ - ነሐሴ) ዓመታዊ የግብር ክፍያ ይከናወናል።",
                    payment.getTenantName(), payment.getAmount().toPlainString(), payment.getPropertyTitle(),
                    payment.getLandlordBankName(), payment.getLandlordAccountNumber(),
                    taxAmt.toPlainString(), bracketRate);

            createAndSendNotification(payment.getLandlordEmail(), NotificationType.RENT_PAYMENT_RECEIVED, "PAYMENT",
                    payment.getTxRef(), landlordMsg);
        } catch (Exception e) {
            log.warn("Failed to dispatch SSE notifications for completed payment {}: {}", payment.getTxRef(), e.getMessage());
        }
    }

    private void createAndSendNotification(String recipientEmail, NotificationType type, String module, String entityId, String msg) {
        if (recipientEmail == null) return;
        try {
            Notification notification = new Notification();
            notification.setRecipientUserId(recipientEmail);
            notification.setType(type);
            notification.setModule(module);
            notification.setEntityId(entityId);
            notification.setMessage(msg);
            notification.setChannel(NotificationChannel.IN_APP);
            notification.setRead(false);
            notification.setCreatedAt(Instant.now());

            notificationRepository.save(notification);

            NotificationResponse response = notificationService.toNotificationResponse(notification);
            sseController.sendNotificationToUser(recipientEmail, response);
            long unread = notificationService.getUnreadCount(recipientEmail);
            sseController.sendUnreadCountUpdate(recipientEmail, unread);
        } catch (Exception e) {
            log.debug("Notification emission skipped for {}: {}", recipientEmail, e.getMessage());
        }
    }

    private PaymentResponse toResponse(Payment p) {
        return PaymentResponse.builder()
                .id(p.getId())
                .txRef(p.getTxRef())
                .chapaReference(p.getChapaReference())
                .agreementId(p.getAgreement() != null ? p.getAgreement().getId() : null)
                .agreementNumber(p.getAgreementNumber())
                .requestCode(p.getRequestCode())
                .propertyTitle(p.getPropertyTitle())
                .tenantName(p.getTenantName())
                .tenantEmail(p.getTenantEmail())
                .landlordName(p.getLandlordName())
                .landlordEmail(p.getLandlordEmail())
                .landlordBankName(p.getLandlordBankName())
                .landlordAccountNumber(p.getLandlordAccountNumber())
                .landlordAccountHolderName(p.getLandlordAccountHolderName())
                .amount(p.getAmount())
                .taxAmount(p.getTaxAmount())
                .netLandlordAmount(p.getNetLandlordAmount())
                .currency(p.getCurrency())
                .status(p.getStatus())
                .paymentMethod(p.getPaymentMethod())
                .checkoutUrl(p.getCheckoutUrl())
                .paymentDate(p.getPaymentDate())
                .createdAt(p.getCreatedAt())
                .build();
    }
}
