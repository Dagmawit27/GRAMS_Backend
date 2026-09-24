package com.ethiorental.backend.tax.service;

import com.ethiorental.backend.IAM.entity.Citizen;
import com.ethiorental.backend.IAM.repository.CitizenRepository;
import com.ethiorental.backend.agreement.entity.Agreement;
import com.ethiorental.backend.agreement.enums.AgreementStatus;
import com.ethiorental.backend.agreement.repository.AgreementRepository;
import com.ethiorental.backend.notification.controller.NotificationSseController;
import com.ethiorental.backend.notification.repository.NotificationRepository;
import com.ethiorental.backend.notification.service.NotificationService;
import com.ethiorental.backend.payment.entity.Payment;
import com.ethiorental.backend.payment.enums.PaymentStatus;
import com.ethiorental.backend.payment.repository.PaymentRepository;
import com.ethiorental.backend.tax.entity.LandlordTax;
import com.ethiorental.backend.tax.repository.LandlordTaxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaxServiceTest {

    @Mock
    private LandlordTaxRepository landlordTaxRepository;

    @Mock
    private AgreementRepository agreementRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private CitizenRepository citizenRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private NotificationSseController sseController;

    private TaxService taxService;

    @BeforeEach
    void setUp() {
        taxService = new TaxService(
                landlordTaxRepository,
                agreementRepository,
                paymentRepository,
                citizenRepository,
                notificationRepository,
                notificationService,
                sseController
        );
    }

    @Test
    void calculateScheduleBAnnualTax_withFiftyPercentDeductionAndBrackets() {
        // Example: ETB 200,000 annual gross income
        // 50% deduction -> Net taxable = 100,000 ETB
        // Brackets:
        // 0 - 24,000: 0% = 0
        // 24,001 - 48,000: 24,000 * 15% = 3,600
        // 48,001 - 84,000: 36,000 * 20% = 7,200
        // 84,001 - 100,000: 16,000 * 25% = 4,000
        // Total Tax = 3,600 + 7,200 + 4,000 = 14,800.00 ETB
        BigDecimal annualGross = BigDecimal.valueOf(200000.00);
        BigDecimal calculatedTax = taxService.calculateScheduleBAnnualTax(annualGross);

        assertThat(calculatedTax).isEqualByComparingTo(BigDecimal.valueOf(14800.00));

        // Bracket check for 200,000 gross (100,000 net) -> 25%
        int bracket = taxService.getScheduleBBracketRate(annualGross);
        assertThat(bracket).isEqualTo(25);
    }

    @Test
    void calculateScheduleBAnnualTax_taxFreeUnderTwentyFourThousandNet() {
        // Gross 40,000 -> 50% deduction = 20,000 taxable -> 0% tax free
        BigDecimal tax = taxService.calculateScheduleBAnnualTax(BigDecimal.valueOf(40000.00));
        assertThat(tax).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(taxService.getScheduleBBracketRate(BigDecimal.valueOf(40000.00))).isEqualTo(0);
    }

    @Test
    void onAgreementActivated_aggregatesAllActiveAgreementsAndContractedMoneyForLandlord() {
        String landlordEmail = "landlord@addisrental.et";
        Citizen landlord = Citizen.builder()
                .id(UUID.randomUUID())
                .email(landlordEmail)
                .firstName("Abebe")
                .lastName("Kebede")
                .build();

        // Agreement 1: 15,000 ETB/mo
        Agreement agreement1 = Agreement.builder()
                .agreementNumber("AGR-001")
                .landlord(landlord)
                .monthlyRent(BigDecimal.valueOf(15000.00))
                .status(AgreementStatus.ACTIVE)
                .build();

        // Agreement 2: 25,000 ETB/mo
        Agreement agreement2 = Agreement.builder()
                .agreementNumber("AGR-002")
                .landlord(landlord)
                .monthlyRent(BigDecimal.valueOf(25000.00))
                .status(AgreementStatus.ACTIVE)
                .build();

        when(agreementRepository.findByLandlordEmailAndStatus(eq(landlordEmail), eq(AgreementStatus.ACTIVE)))
                .thenReturn(List.of(agreement1, agreement2));

        LandlordTax existingRecord = LandlordTax.builder()
                .landlord(landlord)
                .landlordEmail(landlordEmail)
                .fiscalYear(TaxService.CURRENT_FISCAL_YEAR)
                .totalAgreements(0)
                .totalContractedMonthlyRent(BigDecimal.ZERO)
                .projectedAnnualGrossIncome(BigDecimal.ZERO)
                .totalGrossIncome(BigDecimal.ZERO)
                .totalTaxAccrued(BigDecimal.ZERO)
                .totalMonthsPaid(0)
                .taxStatus("ACCRUING")
                .build();

        when(landlordTaxRepository.findByLandlordEmailAndFiscalYear(eq(landlordEmail), eq(TaxService.CURRENT_FISCAL_YEAR)))
                .thenReturn(Optional.of(existingRecord));
        when(landlordTaxRepository.save(any(LandlordTax.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Trigger activation of new agreement
        LandlordTax updated = taxService.onAgreementActivated(agreement2);

        assertThat(updated).isNotNull();
        // 2 active agreements
        assertThat(updated.getTotalAgreements()).isEqualTo(2);
        // Total monthly rent = 15,000 + 25,000 = 40,000 ETB
        assertThat(updated.getTotalContractedMonthlyRent()).isEqualByComparingTo(BigDecimal.valueOf(40000.00));
        // Projected annual gross income = 40,000 * 12 = 480,000 ETB
        assertThat(updated.getProjectedAnnualGrossIncome()).isEqualByComparingTo(BigDecimal.valueOf(480000.00));
        // Projected tax bracket: 480,000 gross -> 240,000 net (> 168,000) -> 35%
        assertThat(updated.getTaxBracketPercentage()).isEqualTo(35);
    }

    @Test
    void onPaymentCompleted_updatesLandlordTaxRevenueAndAccruedTax() {
        String landlordEmail = "landlord@addisrental.et";
        Citizen landlord = Citizen.builder()
                .id(UUID.randomUUID())
                .email(landlordEmail)
                .firstName("Almaz")
                .lastName("Tadesse")
                .build();

        Agreement agreement = Agreement.builder()
                .agreementNumber("AGR-100")
                .landlord(landlord)
                .monthlyRent(BigDecimal.valueOf(20000.00))
                .status(AgreementStatus.ACTIVE)
                .build();

        Payment payment = Payment.builder()
                .txRef("TX-12345")
                .agreement(agreement)
                .landlord(landlord)
                .landlordEmail(landlordEmail)
                .amount(BigDecimal.valueOf(40000.00))
                .status(PaymentStatus.COMPLETED)
                .paymentDate(LocalDateTime.now())
                .build();

        when(agreementRepository.findByLandlordEmailAndStatus(eq(landlordEmail), eq(AgreementStatus.ACTIVE)))
                .thenReturn(List.of(agreement));

        LandlordTax taxRecord = LandlordTax.builder()
                .landlord(landlord)
                .landlordEmail(landlordEmail)
                .fiscalYear(TaxService.CURRENT_FISCAL_YEAR)
                .totalAgreements(1)
                .totalContractedMonthlyRent(BigDecimal.valueOf(20000.00))
                .projectedAnnualGrossIncome(BigDecimal.valueOf(240000.00))
                .totalGrossIncome(BigDecimal.ZERO)
                .totalTaxAccrued(BigDecimal.ZERO)
                .totalMonthsPaid(0)
                .taxStatus("ACCRUING")
                .build();

        when(landlordTaxRepository.findByLandlordEmailAndFiscalYear(eq(landlordEmail), eq(TaxService.CURRENT_FISCAL_YEAR)))
                .thenReturn(Optional.of(taxRecord));
        when(landlordTaxRepository.save(any(LandlordTax.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Advance payment of 2 months = 40,000 ETB
        LandlordTax updated = taxService.onPaymentCompleted(agreement, payment, 2);

        assertThat(updated).isNotNull();
        assertThat(updated.getTotalGrossIncome()).isEqualByComparingTo(BigDecimal.valueOf(40000.00));
        assertThat(updated.getTotalMonthsPaid()).isEqualTo(2);
        assertThat(updated.getTotalTaxAccrued()).isGreaterThan(BigDecimal.ZERO);
        assertThat(updated.getNetIncomeAfterTax()).isEqualByComparingTo(updated.getTotalGrossIncome().subtract(updated.getTotalTaxAccrued()));
        verify(landlordTaxRepository, times(1)).save(any(LandlordTax.class));
    }
}
