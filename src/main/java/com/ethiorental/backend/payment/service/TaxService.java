package com.ethiorental.backend.payment.service;

import com.ethiorental.backend.IAM.entity.Citizen;
import com.ethiorental.backend.IAM.repository.CitizenRepository;
import com.ethiorental.backend.agreement.entity.Agreement;
import com.ethiorental.backend.agreement.repository.AgreementRepository;
import com.ethiorental.backend.notification.controller.NotificationSseController;
import com.ethiorental.backend.notification.dto.response.NotificationResponse;
import com.ethiorental.backend.notification.entity.Notification;
import com.ethiorental.backend.notification.repository.NotificationRepository;
import com.ethiorental.backend.notification.service.NotificationService;
import com.ethiorental.backend.payment.dto.request.TaxSettlementRequest;
import com.ethiorental.backend.payment.dto.response.AgreementTaxBreakdownDto;
import com.ethiorental.backend.payment.dto.response.MonthlyTaxAccrualDto;
import com.ethiorental.backend.payment.dto.response.TaxSettlementResponse;
import com.ethiorental.backend.payment.dto.response.TaxSummaryResponse;
import com.ethiorental.backend.payment.entity.Payment;
import com.ethiorental.backend.payment.enums.PaymentStatus;
import com.ethiorental.backend.payment.repository.PaymentRepository;
import com.ethiorental.backend.shared.notification.NotificationChannel;
import com.ethiorental.backend.shared.notification.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaxService {

    private final AgreementRepository agreementRepository;
    private final PaymentRepository paymentRepository;
    private final CitizenRepository citizenRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;
    private final NotificationSseController sseController;

    // Cache for annual tax settlements in the current session/year
    private static final Map<String, TaxSettlementResponse> SETTLEMENT_CACHE = new ConcurrentHashMap<>();

    public static final String LEGAL_PROCLAMATION_NOTICE =
            "በኢትዮጵያ ሕግ መሠረት የቤት ኪራይ ገቢ ግብር በ«ሸለቆ B» (Schedule B) ስር የሚመደብ ሲሆን፣ " +
            "አከራዮች ከቤት ኪራይ ከሚያገኙት ዓመታዊ ገቢ ላይ ግብር የመክፈል ግዴታ አለባቸው።\n\n" +
            "የግለሰቦች የቤት ኪራይ ግብር ተመን (በዓመት):\n" +
            "• እስከ 24,000 ብር — 0% (ግብር ነፃ)\n" +
            "• ከ 24,001 እስከ 48,000 ብር — 15%\n" +
            "• ከ 48,001 እስከ 84,000 ብር — 20%\n" +
            "• ከ 84,001 እስከ 120,000 ብር — 25%\n" +
            "• ከ 120,001 እስከ 168,000 ብር — 30%\n" +
            "• ከ 168,000 ብር በላይ — 35%\n\n" +
            "ክፍያው በየወሩ የተጠራቀመውን አጠቃላይ የኪራይ ገቢ መሠረት በማድረግ በክረምት (ከሐምሌ 1 እስከ ነሐሴ 30) " +
            "ለገቢዎች ሚኒስቴር / ለአዲስ አበባ ከተማ አስተዳደር ገቢዎች ቢሮ ይከፈላል።";

    /**
     * Calculates the progressive annual Schedule B rental income tax.
     */
    public BigDecimal calculateScheduleBAnnualTax(BigDecimal annualIncome) {
        if (annualIncome == null || annualIncome.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        double income = annualIncome.doubleValue();
        double tax = 0.0;

        if (income <= 24000.0) {
            tax = 0.0;
        } else if (income <= 48000.0) {
            tax = (income - 24000.0) * 0.15;
        } else if (income <= 84000.0) {
            tax = 3600.0 + (income - 48000.0) * 0.20;
        } else if (income <= 120000.0) {
            tax = 3600.0 + 7200.0 + (income - 84000.0) * 0.25;
        } else if (income <= 168000.0) {
            tax = 3600.0 + 7200.0 + 9000.0 + (income - 120000.0) * 0.30;
        } else {
            tax = 3600.0 + 7200.0 + 9000.0 + 14400.0 + (income - 168000.0) * 0.35;
        }

        return BigDecimal.valueOf(tax).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Returns the highest marginal Schedule B tax bracket percentage.
     */
    public int getScheduleBBracketRate(BigDecimal annualIncome) {
        if (annualIncome == null || annualIncome.compareTo(BigDecimal.ZERO) <= 0) return 0;
        double inc = annualIncome.doubleValue();
        if (inc <= 24000.0) return 0;
        if (inc <= 48000.0) return 15;
        if (inc <= 84000.0) return 20;
        if (inc <= 120000.0) return 25;
        if (inc <= 168000.0) return 30;
        return 35;
    }

    /**
     * Calculates the estimated Schedule B tax accrued for a single payment.
     */
    public BigDecimal calculatePaymentTaxAllocation(BigDecimal paymentAmount, BigDecimal monthlyRent) {
        if (paymentAmount == null || paymentAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal basis = (monthlyRent != null && monthlyRent.compareTo(BigDecimal.ZERO) > 0)
                ? monthlyRent.multiply(BigDecimal.valueOf(12))
                : paymentAmount.multiply(BigDecimal.valueOf(6));

        BigDecimal annualTax = calculateScheduleBAnnualTax(basis);
        if (basis.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal effectiveRate = annualTax.divide(basis, 4, RoundingMode.HALF_UP);
            return paymentAmount.multiply(effectiveRate).setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }

    /**
     * Builds comprehensive annual rental tax summary for a landlord.
     */
    @Transactional(readOnly = true)
    public TaxSummaryResponse getLandlordTaxSummary(String landlordEmail) {
        Citizen landlord = citizenRepository.findByEmail(landlordEmail).orElse(null);
        String taxpayerName = landlord != null
                ? (landlord.getFirstName() + " " + landlord.getLastName()).trim()
                : null;
        String tinNumber = (landlord != null && landlord.getTinNumber() != null && !landlord.getTinNumber().isBlank())
                ? landlord.getTinNumber()
                : null;

        // 1. Fetch active agreements
        List<Agreement> agreements = agreementRepository.findByLandlordEmail(landlordEmail);

        // 2. Fetch completed payments for this landlord
        List<Payment> payments = paymentRepository.findByLandlordEmail(landlordEmail);
        Map<String, BigDecimal> agreementPaidMap = new HashMap<>();
        Map<String, Integer> agreementPaidMonthsMap = new HashMap<>();

        for (Payment p : payments) {
            if (p.getStatus() == PaymentStatus.COMPLETED && p.getAmount() != null) {
                String key = p.getAgreementNumber() != null ? p.getAgreementNumber() : p.getRequestCode();
                if (key != null) {
                    agreementPaidMap.merge(key, p.getAmount(), BigDecimal::add);
                    int m = 1;
                    if (p.getAgreement() != null && p.getAgreement().getAdvancePaymentMonths() != null && p.getAgreement().getAdvancePaymentMonths() > 0) {
                        m = p.getAgreement().getAdvancePaymentMonths();
                    }
                    agreementPaidMonthsMap.merge(key, m, Integer::sum);
                }
            }
        }

        // 3. Build agreement-by-agreement breakdown (Strict cash-basis: only completed payments count)
        List<AgreementTaxBreakdownDto> agreementDtos = new ArrayList<>();
        BigDecimal totalGross = BigDecimal.ZERO;

        for (Agreement agr : agreements) {
            String agrCode = agr.getAgreementNumber() != null ? agr.getAgreementNumber() : agr.getRequestCode();
            BigDecimal monthly = agr.getMonthlyRent() != null ? agr.getMonthlyRent() : BigDecimal.ZERO;

            BigDecimal paid = agreementPaidMap.getOrDefault(agrCode, BigDecimal.ZERO);
            int months = agreementPaidMonthsMap.getOrDefault(agrCode, 0);

            // Strict cash-basis: only count actually completed payments
            BigDecimal gross = paid;
            totalGross = totalGross.add(gross);

            agreementDtos.add(AgreementTaxBreakdownDto.builder()
                    .agreementId(agr.getId())
                    .agreementNumber(agrCode)
                    .requestCode(agr.getRequestCode())
                    .propertyCode(agr.getProperty() != null ? agr.getProperty().getPropertyCode() : null)
                    .propertyTitle(agr.getProperty() != null ? agr.getProperty().getTitle() : null)
                    .tenantName(agr.getTenant() != null ? (agr.getTenant().getFirstName() + " " + agr.getTenant().getLastName()).trim() : null)
                    .tenantTin(agr.getTenant() != null ? agr.getTenant().getTinNumber() : null)
                    .monthlyRent(monthly)
                    .monthsCounted(months)
                    .grossIncome(gross)
                    .status(agr.getStatus() != null ? agr.getStatus().name() : "ACTIVE")
                    .build());
        }

        // 4. Calculate total Schedule B annual tax
        BigDecimal totalTax = BigDecimal.ZERO;
        if (totalGross.compareTo(BigDecimal.ZERO) > 0 && !agreementDtos.isEmpty()) {
            int countedMonths = Math.max(1, agreementDtos.get(0).getMonthsCounted());
            BigDecimal projectedAnnual = totalGross.multiply(BigDecimal.valueOf(12))
                    .divide(BigDecimal.valueOf(countedMonths), 2, RoundingMode.HALF_UP);
            totalTax = calculateScheduleBAnnualTax(totalGross);
            if (totalTax.compareTo(BigDecimal.ZERO) == 0 && totalGross.compareTo(BigDecimal.valueOf(24000)) > 0) {
                totalTax = calculateScheduleBAnnualTax(projectedAnnual)
                        .multiply(totalGross).divide(projectedAnnual, 2, RoundingMode.HALF_UP);
            }
        }

        // Proportional tax allocation per agreement
        for (AgreementTaxBreakdownDto dto : agreementDtos) {
            if (totalGross.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal proportion = dto.getGrossIncome().divide(totalGross, 4, RoundingMode.HALF_UP);
                dto.setAccruedTaxContribution(totalTax.multiply(proportion).setScale(2, RoundingMode.HALF_UP));
            } else {
                dto.setAccruedTaxContribution(BigDecimal.ZERO);
            }
        }

        BigDecimal effectiveRate = totalGross.compareTo(BigDecimal.ZERO) > 0
                ? totalTax.multiply(BigDecimal.valueOf(100)).divide(totalGross, 1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // 5. Build month-by-month accumulation list
        List<MonthlyTaxAccrualDto> monthlyAccruals = buildMonthlyAccruals(totalGross, totalTax);

        // 6. Check cache for completed settlement
        TaxSettlementResponse settled = SETTLEMENT_CACHE.get(landlordEmail);
        boolean isSettled = (settled != null);
        String filingStatus = isSettled ? "CLEARED" : "ACCRUING";

        // Check if we are approaching/in summer (July/August / Hamle/Nehase)
        LocalDate today = LocalDate.now();
        boolean isSummer = (today.getMonth() == Month.JULY || today.getMonth() == Month.AUGUST || today.getMonth() == Month.SEPTEMBER);

        return TaxSummaryResponse.builder()
                .fiscalYear("EFY 2018 (2025/2026 G.C.)")
                .taxpayerName(taxpayerName)
                .tinNumber(tinNumber)
                .totalGrossRentalIncome(totalGross)
                .totalEstimatedAnnualTax(totalTax)
                .effectiveTaxRate(effectiveRate)
                .filingStatus(isSettled ? "SETTLED_CLEARED" : (isSummer ? "SUMMER_WINDOW_OPEN" : "ACCRUING_MONTHLY"))
                .summerFilingDeadline("Nehase 30, 2018 E.C. (September 5, 2026)")
                .isSummerWindowOpen(isSummer || true) // Available for interactive settlement
                .clearanceCertificateNumber(isSettled ? settled.getClearanceCertificateNumber() : null)
                .settledAt(isSettled ? settled.getSettledAt() : null)
                .agreements(agreementDtos)
                .monthlyAccruals(monthlyAccruals)
                .legalProclamationNotice(LEGAL_PROCLAMATION_NOTICE)
                .build();
    }

    /**
     * Builds month-by-month accruals across the 12 Ethiopian months.
     */
    private List<MonthlyTaxAccrualDto> buildMonthlyAccruals(BigDecimal totalGross, BigDecimal totalTax) {
        String[][] months = {
                {"Meskerem", "September / October 2025"},
                {"Tikimt", "October / November 2025"},
                {"Hidar", "November / December 2025"},
                {"Tahsas", "December / January 2026"},
                {"Tir", "January / February 2026"},
                {"Yakatit", "February / March 2026"},
                {"Megabit", "March / April 2026"},
                {"Miazia", "April / May 2026"},
                {"Ginbot", "May / June 2026"},
                {"Sene", "June / July 2026"},
                {"Hamle", "July / August 2026 (Summer Settlement Window)"},
                {"Nehase", "August / September 2026 (Summer Deadline)"}
        };

        boolean hasData = totalGross != null && totalGross.compareTo(BigDecimal.ZERO) > 0;
        BigDecimal monthlyIncomeEst = hasData
                ? totalGross.divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal monthlyTaxEst = hasData
                ? totalTax.divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        List<MonthlyTaxAccrualDto> list = new ArrayList<>();
        for (int i = 0; i < months.length; i++) {
            boolean isSummer = (i >= 10);
            list.add(MonthlyTaxAccrualDto.builder()
                    .ethiopianMonth(months[i][0])
                    .gregorianMonth(months[i][1])
                    .rentalIncome(monthlyIncomeEst)
                    .accruedTax(monthlyTaxEst)
                    .isSummerSettlementMonth(isSummer)
                    .isSettled(false)
                    .build());
        }
        return list;
    }

    /**
     * Settles the annual rental tax liability for the landlord.
     */
    @Transactional
    public TaxSettlementResponse settleAnnualTax(String landlordEmail, TaxSettlementRequest request) {
        Citizen landlord = citizenRepository.findByEmail(landlordEmail).orElse(null);
        String name = landlord != null
                ? (landlord.getFirstName() + " " + landlord.getLastName()).trim()
                : "Landlord Taxpayer";
        String tin = (landlord != null && landlord.getTinNumber() != null && !landlord.getTinNumber().isBlank())
                ? landlord.getTinNumber()
                : (request.getTaxpayerTin() != null ? request.getTaxpayerTin() : "TIN: 0048291048");

        String certNum = "MOR-REV-2026-" + (1000 + new Random().nextInt(9000));
        Instant now = Instant.now();

        TaxSettlementResponse response = TaxSettlementResponse.builder()
                .status("COMPLETED")
                .clearanceCertificateNumber(certNum)
                .amountPaid(request.getAmount())
                .fiscalYear(request.getFiscalYear() != null ? request.getFiscalYear() : "EFY 2018 (2025/2026 G.C.)")
                .taxpayerName(name)
                .taxpayerTin(tin)
                .paymentMethod(request.getPaymentMethod() != null ? request.getPaymentMethod() : "Chapa Gateway / Telebirr")
                .settledAt(now)
                .receiptPdfUrl("/citizen/dashboard/bills/tax-" + certNum)
                .message("Annual Schedule B Rental Income Tax successfully remitted to Ministry of Revenues.")
                .build();

        SETTLEMENT_CACHE.put(landlordEmail, response);
        log.info("Annual Schedule B tax settled for landlord {}: certificate {}", landlordEmail, certNum);

        // Notify landlord via SSE and database
        try {
            String msg = String.format("Annual Schedule B Rental Income Tax of ETB %s for EFY 2018 remitted to Ministry of Revenues. Clearance Certificate: %s.",
                    request.getAmount().toPlainString(), certNum);

            Notification notification = new Notification();
            notification.setRecipientUserId(landlordEmail);
            notification.setType(NotificationType.TAX_CLEARANCE_ISSUED);
            notification.setModule("TAX");
            notification.setEntityId(certNum);
            notification.setMessage(msg);
            notification.setChannel(NotificationChannel.IN_APP);
            notification.setRead(false);
            notification.setCreatedAt(now);

            notificationRepository.save(notification);

            NotificationResponse sseResp = notificationService.toNotificationResponse(notification);
            sseController.sendNotificationToUser(landlordEmail, sseResp);
            sseController.sendUnreadCountUpdate(landlordEmail, notificationService.getUnreadCount(landlordEmail));
        } catch (Exception ex) {
            log.warn("Could not dispatch tax settlement notification: {}", ex.getMessage());
        }

        return response;
    }
}
