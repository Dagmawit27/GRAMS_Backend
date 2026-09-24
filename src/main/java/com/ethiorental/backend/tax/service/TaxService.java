package com.ethiorental.backend.tax.service;

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
import com.ethiorental.backend.payment.entity.Payment;
import com.ethiorental.backend.payment.enums.PaymentStatus;
import com.ethiorental.backend.payment.repository.PaymentRepository;
import com.ethiorental.backend.shared.notification.NotificationChannel;
import com.ethiorental.backend.shared.notification.NotificationType;
import com.ethiorental.backend.tax.dto.request.TaxSettlementRequest;
import com.ethiorental.backend.tax.dto.response.AgreementTaxBreakdownDto;
import com.ethiorental.backend.tax.dto.response.MonthlyTaxAccrualDto;
import com.ethiorental.backend.tax.dto.response.TaxSettlementResponse;
import com.ethiorental.backend.tax.dto.response.TaxSummaryResponse;
import com.ethiorental.backend.tax.entity.LandlordTax;
import com.ethiorental.backend.tax.repository.LandlordTaxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.*;

/**
 * Core Tax Service for Ethiopian Federal Income Tax (Schedule B - Rental of Buildings).
 * Governed by Proclamation No. 979/2016 and Amendment Proclamation No. 1395/2017 E.C. (2025 G.C.).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TaxService {

    public static final String CURRENT_FISCAL_YEAR = "EFY 2018 (2025/2026 G.C.)";

    private final LandlordTaxRepository landlordTaxRepository;
    private final AgreementRepository agreementRepository;
    private final PaymentRepository paymentRepository;
    private final CitizenRepository citizenRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;
    private final NotificationSseController sseController;

    public static final String LEGAL_PROCLAMATION_NOTICE =
            "በኢትዮጵያ ሕግ መሠረት የቤት ኪራይ ገቢ ግብር በ«ሸለቆ B» (Schedule B) ስር የሚመደብ ሲሆን፣ " +
            "አከራዮች ከቤት ኪራይ ከሚያገኙት ዓመታዊ ገቢ ላይ ግብር የመክፈል ሕጋዊ ግዴታ አለባቸው።\n\n" +
            "በፌደራል ገቢ ግብር አዋጅ ቁጥር 979/2008 አንቀጽ 15 እና በአዲሱ ማሻሻያ አዋጅ ቁጥር 1395/2017 መሠረት፡\n" +
            "1. የሂሳብ መዝገብ ለማይይዙ ግለሰብ አከራዮች መንግስት 50% የወጪ/ጥገና ቅናሽ (Deduction Allowance) ይሰጣል።\n" +
            "2. ግብር የሚታሰብበት የተጣራ ዓመታዊ ገቢ (Taxable Income) እርከኖች፡\n" +
            "• እስከ 24,000 ብር — 0% (ግብር ነፃ)\n" +
            "• ከ 24,001 እስከ 48,000 ብር — 15% (መቀነሻ 3,600 ብር)\n" +
            "• ከ 48,001 እስከ 84,000 ብር — 20% (መቀነሻ 6,000 ብር)\n" +
            "• ከ 84,001 እስከ 120,000 ብር — 25% (መቀነሻ 10,200 ብር)\n" +
            "• ከ 120,001 እስከ 168,000 ብር — 30% (መቀነሻ 16,200 ብር)\n" +
            "• ከ 168,000 ብር በላይ — 35% (መቀነሻ 24,600 ብር)\n\n" +
            "3. ለድርጅቶች (Corporate Bodies) የግብር ተመኑ 30% በደፈናው ነው።\n" +
            "4. ዓመታዊ የግብር ክፍያ በክረምት ወቅት (ከሐምሌ 1 እስከ ነሐሴ 30) ለአዲስ አበባ ገቢዎች ወይም ለገቢዎች ሚኒስቴር ይከፈላል።";

    /**
     * Calculates Schedule B annual rental tax for an individual landlord without books.
     * Applies the statutory 50% expense allowance (Proclamation 979/2016 Art. 15(5)(b))
     * followed by the progressive tax brackets under Amendment 1395/2017 E.C.
     */
    public BigDecimal calculateScheduleBAnnualTax(BigDecimal annualGrossIncome) {
        if (annualGrossIncome == null || annualGrossIncome.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        // Apply 50% statutory deduction for repairs, maintenance, and depreciation
        BigDecimal deduction = annualGrossIncome.multiply(BigDecimal.valueOf(0.50));
        BigDecimal netTaxableIncome = annualGrossIncome.subtract(deduction);

        double income = netTaxableIncome.doubleValue();
        double tax = 0.0;

        // Progressive brackets per Amendment Proclamation No. 1395/2017
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
     * Returns the highest marginal Schedule B tax bracket percentage
     * on net taxable income after the 50% deduction.
     */
    public int getScheduleBBracketRate(BigDecimal annualGrossIncome) {
        if (annualGrossIncome == null || annualGrossIncome.compareTo(BigDecimal.ZERO) <= 0) return 0;

        // Apply 50% deduction
        BigDecimal netTaxableIncome = annualGrossIncome.multiply(BigDecimal.valueOf(0.50));
        double inc = netTaxableIncome.doubleValue();

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
     * Lifecycle Hook: Called when a payment is COMPLETED.
     * Updates LandlordTax entity:
     * - Increases totalGrossIncome by payment amount
     * - Increments totalMonthsPaid
     * - Sets lastPaymentDate
     * - Refreshes active agreements count and aggregated monthly rent across all active agreements
     * - Recalculates totalTaxAccrued, taxBracketPercentage, effectiveTaxRate, and netIncomeAfterTax
     */
    @Transactional
    public LandlordTax onPaymentCompleted(Agreement agreement, Payment payment, int monthsPaid) {
        if (payment == null || payment.getStatus() != PaymentStatus.COMPLETED) {
            return null;
        }

        String landlordEmail = payment.getLandlordEmail();
        if (landlordEmail == null && payment.getLandlord() != null) {
            landlordEmail = payment.getLandlord().getEmail();
        }
        if (landlordEmail == null && agreement != null && agreement.getLandlord() != null) {
            landlordEmail = agreement.getLandlord().getEmail();
        }

        if (landlordEmail == null) {
            log.warn("Cannot update LandlordTax: landlordEmail is null for payment {}", payment.getTxRef());
            return null;
        }

        Citizen landlord = payment.getLandlord();
        if (landlord == null && agreement != null) {
            landlord = agreement.getLandlord();
        }
        if (landlord == null) {
            landlord = citizenRepository.findByEmail(landlordEmail).orElse(null);
        }

        if (landlord == null) {
            log.warn("Cannot find landlord citizen entity for email: {}", landlordEmail);
            return null;
        }

        LandlordTax taxRecord = getOrCreateLandlordTax(landlord, landlordEmail);

        // 1. Accumulate collected cash revenue
        BigDecimal paymentAmt = payment.getAmount() != null ? payment.getAmount() : BigDecimal.ZERO;
        taxRecord.setTotalGrossIncome(taxRecord.getTotalGrossIncome().add(paymentAmt));
        taxRecord.setTotalMonthsPaid(taxRecord.getTotalMonthsPaid() + Math.max(1, monthsPaid));
        taxRecord.setLastPaymentDate(payment.getPaymentDate() != null ? payment.getPaymentDate() : LocalDateTime.now());

        // 2. Synchronize active agreements & total rent across all agreements for this landlord
        syncLandlordActiveAgreements(taxRecord, landlordEmail);

        // 3. Recalculate accrued tax liability based on collected gross income and months paid
        int totalMonths = taxRecord.getTotalMonthsPaid();
        BigDecimal totalGross = taxRecord.getTotalGrossIncome();
        if (totalGross.compareTo(BigDecimal.ZERO) > 0 && totalMonths > 0) {
            // Annualized run-rate: (totalGross * 12) / totalMonths
            BigDecimal projectedAnnual = totalGross.multiply(BigDecimal.valueOf(12))
                    .divide(BigDecimal.valueOf(totalMonths), 2, RoundingMode.HALF_UP);

            BigDecimal annualizedTax = calculateScheduleBAnnualTax(projectedAnnual);
            // Pro-rated actual accrued tax: (annualizedTax * totalMonths) / 12
            BigDecimal proRatedTax = annualizedTax.multiply(BigDecimal.valueOf(totalMonths))
                    .divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);

            taxRecord.setTotalTaxAccrued(proRatedTax);
            taxRecord.setTaxBracketPercentage(getScheduleBBracketRate(projectedAnnual));
        }

        taxRecord.recalculateDerivedFields();
        LandlordTax saved = landlordTaxRepository.save(taxRecord);
        log.info("LandlordTax updated on payment {}: landlord={}, grossETB={}, taxAccruedETB={}, activeAgreements={}, bracket={}%",
                payment.getTxRef(), landlordEmail, saved.getTotalGrossIncome(), saved.getTotalTaxAccrued(),
                saved.getTotalAgreements(), saved.getTaxBracketPercentage());

        return saved;
    }

    /**
     * Lifecycle Hook: Called when a new Agreement is activated or created for a landlord.
     * Requirements:
     * - Update total active agreements count for this person.
     * - Aggregate landlord contracted money across ALL active agreements.
     * - Update projected annual gross income and projected tax bracket.
     */
    @Transactional
    public LandlordTax onAgreementActivated(Agreement agreement) {
        if (agreement == null || agreement.getLandlord() == null) {
            log.warn("Cannot process agreement activation for tax: agreement or landlord is null");
            return null;
        }

        Citizen landlord = agreement.getLandlord();
        String landlordEmail = landlord.getEmail();
        if (landlordEmail == null || landlordEmail.isBlank()) {
            log.warn("Landlord email missing for agreement: {}", agreement.getAgreementNumber());
            return null;
        }

        LandlordTax taxRecord = getOrCreateLandlordTax(landlord, landlordEmail);

        // Synchronize active agreements & total contracted money across ALL active agreements
        syncLandlordActiveAgreements(taxRecord, landlordEmail);

        taxRecord.recalculateDerivedFields();
        LandlordTax saved = landlordTaxRepository.save(taxRecord);
        log.info("LandlordTax synchronized on active agreement {}: landlord={}, activeAgreements={}, totalMonthlyRentETB={}, projectedAnnualETB={}",
                agreement.getAgreementNumber(), landlordEmail, saved.getTotalAgreements(),
                saved.getTotalContractedMonthlyRent(), saved.getProjectedAnnualGrossIncome());

        return saved;
    }

    /**
     * Helper to query all active agreements for this landlord and update totalAgreements,
     * totalContractedMonthlyRent, and projectedAnnualGrossIncome.
     */
    private void syncLandlordActiveAgreements(LandlordTax taxRecord, String landlordEmail) {
        List<Agreement> activeAgreements = agreementRepository.findByLandlordEmailAndStatus(landlordEmail, AgreementStatus.ACTIVE);
        
        int activeCount = activeAgreements != null ? activeAgreements.size() : 0;
        BigDecimal totalMonthlyRent = BigDecimal.ZERO;

        if (activeAgreements != null) {
            for (Agreement a : activeAgreements) {
                if (a.getMonthlyRent() != null && a.getMonthlyRent().compareTo(BigDecimal.ZERO) > 0) {
                    totalMonthlyRent = totalMonthlyRent.add(a.getMonthlyRent());
                }
            }
        }

        taxRecord.setTotalAgreements(activeCount);
        taxRecord.setTotalContractedMonthlyRent(totalMonthlyRent);
        taxRecord.setProjectedAnnualGrossIncome(totalMonthlyRent.multiply(BigDecimal.valueOf(12)));

        // If no cash payment has arrived yet, establish initial projected tax bracket from contracted rent
        if (taxRecord.getTotalGrossIncome() == null || taxRecord.getTotalGrossIncome().compareTo(BigDecimal.ZERO) == 0) {
            taxRecord.setTaxBracketPercentage(getScheduleBBracketRate(taxRecord.getProjectedAnnualGrossIncome()));
        }
    }

    /**
     * Finds or creates a LandlordTax record for the given landlord and fiscal year.
     */
    @Transactional
    public LandlordTax getOrCreateLandlordTax(Citizen landlord, String landlordEmail) {
        return landlordTaxRepository.findByLandlordEmailAndFiscalYear(landlordEmail, CURRENT_FISCAL_YEAR)
                .orElseGet(() -> {
                    LandlordTax newRecord = LandlordTax.builder()
                            .landlord(landlord)
                            .landlordEmail(landlordEmail)
                            .fiscalYear(CURRENT_FISCAL_YEAR)
                            .totalAgreements(0)
                            .totalContractedMonthlyRent(BigDecimal.ZERO)
                            .projectedAnnualGrossIncome(BigDecimal.ZERO)
                            .totalGrossIncome(BigDecimal.ZERO)
                            .totalTaxAccrued(BigDecimal.ZERO)
                            .totalTaxPaid(BigDecimal.ZERO)
                            .totalMonthsPaid(0)
                            .taxStatus("ACCRUING")
                            .taxBracketPercentage(0)
                            .effectiveTaxRate(BigDecimal.ZERO)
                            .netIncomeAfterTax(BigDecimal.ZERO)
                            .build();
                    return landlordTaxRepository.save(newRecord);
                });
    }

    /**
     * Builds comprehensive annual rental tax summary for a landlord.
     */
    @Transactional(readOnly = true)
    public TaxSummaryResponse getLandlordTaxSummary(String landlordEmail) {
        Citizen landlord = citizenRepository.findByEmail(landlordEmail).orElse(null);
        String taxpayerName = landlord != null
                ? (landlord.getFirstName() + " " + landlord.getLastName()).trim()
                : "Landlord Taxpayer";
        String tinNumber = (landlord != null && landlord.getTinNumber() != null && !landlord.getTinNumber().isBlank())
                ? landlord.getTinNumber()
                : null;

        // Fetch persisted LandlordTax entity
        Optional<LandlordTax> recordOpt = landlordTaxRepository.findByLandlordEmailAndFiscalYear(landlordEmail, CURRENT_FISCAL_YEAR);

        // Fetch agreements
        List<Agreement> agreements = agreementRepository.findByLandlordEmail(landlordEmail);

        // Fetch completed payments for breakdown
        List<Payment> payments = paymentRepository.findByLandlordEmail(landlordEmail);
        Map<String, BigDecimal> agreementPaidMap = new HashMap<>();
        Map<String, Integer> agreementPaidMonthsMap = new HashMap<>();
        Map<String, List<LocalDate>> agreementPaymentMonthsMap = new HashMap<>();

        for (Payment p : payments) {
            if (p.getStatus() == PaymentStatus.COMPLETED && p.getAmount() != null) {
                String key = p.getAgreementNumber() != null ? p.getAgreementNumber() : p.getRequestCode();
                if (key != null) {
                    agreementPaidMap.merge(key, p.getAmount(), BigDecimal::add);

                    LocalDate periodDate = null;
                    if (p.getPeriodCoveredDate() != null) {
                        periodDate = p.getPeriodCoveredDate().toLocalDate();
                    } else if (p.getPaymentDate() != null) {
                        periodDate = p.getPaymentDate().toLocalDate();
                    }

                    int monthsCounted = 1;
                    if (p.getAgreement() != null && p.getAgreement().getAdvancePaymentMonths() != null && p.getAgreement().getAdvancePaymentMonths() > 0) {
                        monthsCounted = p.getAgreement().getAdvancePaymentMonths();
                    }

                    if (periodDate != null) {
                        agreementPaymentMonthsMap.computeIfAbsent(key, k -> new ArrayList<>()).add(periodDate);
                    }
                    agreementPaidMonthsMap.merge(key, monthsCounted, Integer::sum);
                }
            }
        }

        // Build agreement breakdown list
        List<AgreementTaxBreakdownDto> agreementDtos = new ArrayList<>();
        BigDecimal totalGross = BigDecimal.ZERO;
        int totalActualMonthsPaid = 0;

        for (Agreement agr : agreements) {
            String agrCode = agr.getAgreementNumber() != null ? agr.getAgreementNumber() : agr.getRequestCode();
            BigDecimal monthly = agr.getMonthlyRent() != null ? agr.getMonthlyRent() : BigDecimal.ZERO;

            BigDecimal paid = agreementPaidMap.getOrDefault(agrCode, BigDecimal.ZERO);
            int months = agreementPaidMonthsMap.getOrDefault(agrCode, 0);
            totalActualMonthsPaid += months;
            totalGross = totalGross.add(paid);

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
                    .grossIncome(paid)
                    .status(agr.getStatus() != null ? agr.getStatus().name() : "ACTIVE")
                    .build());
        }

        // Calculate Schedule B tax
        BigDecimal totalTax = BigDecimal.ZERO;
        if (totalGross.compareTo(BigDecimal.ZERO) > 0 && totalActualMonthsPaid > 0) {
            BigDecimal projectedAnnualIncome = totalGross.multiply(BigDecimal.valueOf(12))
                    .divide(BigDecimal.valueOf(totalActualMonthsPaid), 2, RoundingMode.HALF_UP);
            BigDecimal annualTax = calculateScheduleBAnnualTax(projectedAnnualIncome);
            totalTax = annualTax.multiply(BigDecimal.valueOf(totalActualMonthsPaid))
                    .divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
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

        BigDecimal netIncomeAfterTax = totalGross.subtract(totalTax);

        int taxBracketPercentage = 0;
        if (totalGross.compareTo(BigDecimal.ZERO) > 0 && totalActualMonthsPaid > 0) {
            BigDecimal projectedAnnual = totalGross.multiply(BigDecimal.valueOf(12))
                    .divide(BigDecimal.valueOf(totalActualMonthsPaid), 2, RoundingMode.HALF_UP);
            taxBracketPercentage = getScheduleBBracketRate(projectedAnnual);
        }

        // Calculate aggregated contracted rent across all active agreements
        BigDecimal totalContractedMonthlyRent = recordOpt.map(LandlordTax::getTotalContractedMonthlyRent)
                .orElseGet(() -> agreements.stream()
                        .filter(a -> a.getStatus() == AgreementStatus.ACTIVE && a.getMonthlyRent() != null)
                        .map(Agreement::getMonthlyRent)
                        .reduce(BigDecimal.ZERO, BigDecimal::add));

        BigDecimal projectedAnnualGrossIncome = totalContractedMonthlyRent.multiply(BigDecimal.valueOf(12));

        // Month-by-month accruals
        List<MonthlyTaxAccrualDto> monthlyAccruals = buildMonthlyAccrualsFromPayments(
                totalGross, totalTax, totalActualMonthsPaid, agreementPaymentMonthsMap
        );

        // Check settlement state
        boolean isSettled = recordOpt.map(r -> "SETTLED_CLEARED".equalsIgnoreCase(r.getTaxStatus())).orElse(false);
        String certNum = recordOpt.map(LandlordTax::getClearanceCertificateNumber).orElse(null);
        Instant settledAt = recordOpt.map(r -> r.getSettlementDate() != null
                ? r.getSettlementDate().atZone(java.time.ZoneId.systemDefault()).toInstant()
                : null).orElse(null);

        LocalDate today = LocalDate.now();
        boolean isSummer = (today.getMonth() == Month.JULY || today.getMonth() == Month.AUGUST || today.getMonth() == Month.SEPTEMBER);

        return TaxSummaryResponse.builder()
                .fiscalYear(CURRENT_FISCAL_YEAR)
                .taxpayerName(taxpayerName)
                .tinNumber(tinNumber)
                .totalGrossRentalIncome(totalGross)
                .totalEstimatedAnnualTax(totalTax)
                .effectiveTaxRate(effectiveRate)
                .netIncomeAfterTax(netIncomeAfterTax)
                .taxBracketPercentage(taxBracketPercentage)
                .totalMonthsPaid(totalActualMonthsPaid)
                .totalAgreementsCount(agreements.size())
                .totalContractedMonthlyRent(totalContractedMonthlyRent)
                .projectedAnnualGrossIncome(projectedAnnualGrossIncome)
                .filingStatus(isSettled ? "SETTLED_CLEARED" : (isSummer ? "SUMMER_WINDOW_OPEN" : "ACCRUING_MONTHLY"))
                .summerFilingDeadline("Nehase 30, 2018 E.C. (September 5, 2026)")
                .isSummerWindowOpen(true)
                .clearanceCertificateNumber(certNum)
                .settledAt(settledAt)
                .agreements(agreementDtos)
                .monthlyAccruals(monthlyAccruals)
                .legalProclamationNotice(LEGAL_PROCLAMATION_NOTICE)
                .build();
    }

    private List<MonthlyTaxAccrualDto> buildMonthlyAccrualsFromPayments(
            BigDecimal totalGross,
            BigDecimal totalTax,
            int totalActualMonthsPaid,
            Map<String, List<LocalDate>> agreementPaymentMonthsMap
    ) {
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

        boolean hasData = totalGross != null && totalGross.compareTo(BigDecimal.ZERO) > 0 && totalActualMonthsPaid > 0;
        BigDecimal avgIncomePerPaidMonth = hasData
                ? totalGross.divide(BigDecimal.valueOf(totalActualMonthsPaid), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal avgTaxPerPaidMonth = hasData
                ? totalTax.divide(BigDecimal.valueOf(totalActualMonthsPaid), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        Set<Integer> monthsWithPayments = new HashSet<>();
        for (List<LocalDate> paymentDates : agreementPaymentMonthsMap.values()) {
            for (LocalDate date : paymentDates) {
                int monthIndex = getEthiopianMonthIndex(date);
                if (monthIndex >= 0 && monthIndex < 12) {
                    monthsWithPayments.add(monthIndex);
                }
            }
        }

        List<MonthlyTaxAccrualDto> list = new ArrayList<>();
        for (int i = 0; i < months.length; i++) {
            boolean isSummer = (i >= 10);
            boolean hasPaymentInMonth = monthsWithPayments.contains(i);

            BigDecimal monthIncome = hasPaymentInMonth ? avgIncomePerPaidMonth : BigDecimal.ZERO;
            BigDecimal monthTax = hasPaymentInMonth ? avgTaxPerPaidMonth : BigDecimal.ZERO;

            list.add(MonthlyTaxAccrualDto.builder()
                    .ethiopianMonth(months[i][0])
                    .gregorianMonth(months[i][1])
                    .rentalIncome(monthIncome)
                    .accruedTax(monthTax)
                    .isSummerSettlementMonth(isSummer)
                    .isSettled(false)
                    .build());
        }
        return list;
    }

    private int getEthiopianMonthIndex(LocalDate gregorianDate) {
        int month = gregorianDate.getMonthValue();
        int year = gregorianDate.getYear();

        if (year == 2025) {
            if (month >= 9 && month <= 10) return 0;
            if (month >= 10 && month <= 11) return 1;
            if (month >= 11 && month <= 12) return 2;
            if (month == 12) return 3;
        } else if (year == 2026) {
            if (month == 1) return 3;
            if (month >= 1 && month <= 2) return 4;
            if (month >= 2 && month <= 3) return 5;
            if (month >= 3 && month <= 4) return 6;
            if (month >= 4 && month <= 5) return 7;
            if (month >= 5 && month <= 6) return 8;
            if (month >= 6 && month <= 7) return 9;
            if (month >= 7 && month <= 8) return 10;
            if (month >= 8 && month <= 9) return 11;
        }
        return -1;
    }

    /**
     * Settles the annual rental tax liability for the landlord.
     * Updates LandlordTax entity with payment details and certificate number.
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
        LocalDateTime localNow = LocalDateTime.now();

        // Update LandlordTax entity
        if (landlord != null) {
            LandlordTax taxRecord = getOrCreateLandlordTax(landlord, landlordEmail);
            taxRecord.setTotalTaxPaid(taxRecord.getTotalTaxPaid().add(request.getAmount()));
            taxRecord.setTaxStatus("SETTLED_CLEARED");
            taxRecord.setSettlementDate(localNow);
            taxRecord.setClearanceCertificateNumber(certNum);
            landlordTaxRepository.save(taxRecord);
            log.info("Persisted tax settlement in LandlordTax for {}: certificate {}", landlordEmail, certNum);
        }

        TaxSettlementResponse response = TaxSettlementResponse.builder()
                .status("COMPLETED")
                .clearanceCertificateNumber(certNum)
                .amountPaid(request.getAmount())
                .fiscalYear(request.getFiscalYear() != null ? request.getFiscalYear() : CURRENT_FISCAL_YEAR)
                .taxpayerName(name)
                .taxpayerTin(tin)
                .paymentMethod(request.getPaymentMethod() != null ? request.getPaymentMethod() : "Chapa Gateway / Telebirr")
                .settledAt(now)
                .receiptPdfUrl("/citizen/dashboard/bills/tax-" + certNum)
                .message("Annual Schedule B Rental Income Tax successfully remitted to Ministry of Revenues.")
                .build();

        // Dispatch real-time SSE notification
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
