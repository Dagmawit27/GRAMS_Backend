package com.ethiorental.backend.agreement.service;

import com.ethiorental.backend.IAM.entity.Citizen;
import com.ethiorental.backend.agreement.dto.AgreementResponse;
import com.ethiorental.backend.agreement.entity.Agreement;
import com.ethiorental.backend.agreement.enums.AgreementStatus;
import com.ethiorental.backend.agreement.repository.AgreementRepository;
import com.ethiorental.backend.lease.entity.LeaseRequest;
import com.ethiorental.backend.property.entity.Address;
import com.ethiorental.backend.property.entity.Property;
import com.ethiorental.backend.property.entity.PropertyUnit;
import com.ethiorental.backend.property.enums.PropertyStatus;
import com.ethiorental.backend.property.repository.PropertyRepository;
import com.ethiorental.backend.property.repository.PropertyUnitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgreementServiceImplTest {

    @Mock
    private AgreementRepository agreementRepository;

    @Mock
    private PropertyRepository propertyRepository;

    @Mock
    private PropertyUnitRepository propertyUnitRepository;

    @Mock
    private com.ethiorental.backend.tax.service.TaxService taxService;

    private AgreementServiceImpl agreementService;

    @BeforeEach
    void setUp() {
        agreementService = new AgreementServiceImpl(agreementRepository, propertyRepository, propertyUnitRepository, taxService);
    }

    @Test
    void createAgreementFromLeaseRequest_Success() {
        UUID leaseId = UUID.randomUUID();
        UUID propertyId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        UUID landlordId = UUID.randomUUID();

        Citizen tenant = Citizen.builder()
                .id(tenantId)
                .firstName("Abebe")
                .middleName("Kebede")
                .lastName("Haile")
                .email("abebe@example.com")
                .phone("+251911000001")
                .subCity("Bole")
                .woreda("03")
                .build();

        Citizen landlord = Citizen.builder()
                .id(landlordId)
                .firstName("Almaz")
                .middleName("Tadesse")
                .lastName("Bekele")
                .email("almaz@example.com")
                .phone("+251911000002")
                .subCity("Bole")
                .woreda("03")
                .build();

        Address address = Address.builder()
                .subCity("Bole")
                .woreda("03")
                .build();

        Property property = Property.builder()
                .id(propertyId)
                .propertyCode("PROP-001")
                .title("Luxury Bole Apartment")
                .propertyType("Apartment")
                .address(address)
                .advanceRent(2)
                .status(PropertyStatus.LISTED)
                .build();

        PropertyUnit unit = PropertyUnit.builder()
                .id(UUID.randomUUID())
                .unitCode("U-101")
                .unitName("Apartment 101")
                .build();

        LeaseRequest leaseRequest = LeaseRequest.builder()
                .id(leaseId)
                .requestCode("LR-123456")
                .property(property)
                .unit(unit)
                .applicant(tenant)
                .landlord(landlord)
                .proposedRent(BigDecimal.valueOf(15000))
                .leaseDurationMonths(12)
                .landlordSigned(true)
                .landlordSignedAt(LocalDateTime.now().minusDays(1))
                .tenantSigned(true)
                .tenantSignedAt(LocalDateTime.now().minusDays(1))
                .reviewedAt(LocalDateTime.now().minusHours(2))
                .build();

        when(agreementRepository.findByRequestCode("LR-123456")).thenReturn(Optional.empty());
        when(agreementRepository.save(any(Agreement.class))).thenAnswer(invocation -> {
            Agreement a = invocation.getArgument(0);
            a.setId(UUID.randomUUID());
            return a;
        });

        AgreementResponse response = agreementService.createAgreementFromLeaseRequest(leaseRequest, "supervisor@addis.gov.et");

        assertThat(response).isNotNull();
        assertThat(response.getRequestCode()).isEqualTo("LR-123456");
        assertThat(response.getMonthlyRent()).isEqualByComparingTo("15000");
        assertThat(response.getAdvancePaymentMonths()).isEqualTo(2);
        assertThat(response.getStatus()).isEqualTo("ACTIVE");
        assertThat(response.getTenantEmail()).isEqualTo("abebe@example.com");
        assertThat(response.getLandlordEmail()).isEqualTo("almaz@example.com");
        assertThat(response.getPropertyCode()).isEqualTo("PROP-001");
        assertThat(response.getSupervisorEmail()).isEqualTo("supervisor@addis.gov.et");

        ArgumentCaptor<Agreement> captor = ArgumentCaptor.forClass(Agreement.class);
        verify(agreementRepository).save(captor.capture());
        Agreement savedEntity = captor.getValue();
        assertThat(savedEntity.getProperty().getId()).isEqualTo(propertyId);
        assertThat(savedEntity.getTenant().getId()).isEqualTo(tenantId);
        assertThat(savedEntity.getLandlord().getId()).isEqualTo(landlordId);
        assertThat(savedEntity.getStatus()).isEqualTo(AgreementStatus.ACTIVE);
        assertThat(savedEntity.getTotalMonthsPaid()).isEqualTo(0);
        assertThat(savedEntity.getMonthlyPaymentDueDay()).isEqualTo(LocalDateTime.now().getDayOfMonth());
        assertThat(savedEntity.getNextPaymentDueDate()).isEqualTo(LocalDate.now());
        assertThat(response.getTotalMonthsPaid()).isEqualTo(0);
        assertThat(response.getNextPaymentDueDate()).isEqualTo(LocalDate.now());

        verify(propertyRepository).save(property);
        assertThat(property.getStatus()).isEqualTo(PropertyStatus.RENTED);
    }

    @Test
    void getMyAgreements_ReturnsUserAgreements() {
        Agreement a1 = Agreement.builder()
                .id(UUID.randomUUID())
                .agreementNumber("AGR-001")
                .requestCode("LR-111")
                .status(AgreementStatus.ACTIVE)
                .monthlyRent(BigDecimal.valueOf(10000))
                .build();

        when(agreementRepository.findByLandlordEmailOrTenantEmail("user@example.com"))
                .thenReturn(List.of(a1));

        List<AgreementResponse> results = agreementService.getMyAgreements("user@example.com");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getAgreementNumber()).isEqualTo("AGR-001");
    }

    @Test
    void getAllActiveAgreements_ReturnsAllActiveAgreements() {
        Agreement a1 = Agreement.builder()
                .id(UUID.randomUUID())
                .agreementNumber("AGR-100")
                .requestCode("LR-100")
                .status(AgreementStatus.ACTIVE)
                .monthlyRent(BigDecimal.valueOf(15000))
                .build();

        when(agreementRepository.findByStatusOrderByCreatedAtDesc(AgreementStatus.ACTIVE))
                .thenReturn(List.of(a1));

        List<AgreementResponse> results = agreementService.getAllActiveAgreements();

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getAgreementNumber()).isEqualTo("AGR-100");
    }

    @Test
    void tenantCancelAgreement_Success() {
        Citizen tenant = Citizen.builder()
                .id(UUID.randomUUID())
                .email("tenant@example.com")
                .build();

        Property property = Property.builder()
                .id(UUID.randomUUID())
                .status(PropertyStatus.RENTED)
                .build();

        PropertyUnit unit = PropertyUnit.builder()
                .id(UUID.randomUUID())
                .status(com.ethiorental.backend.property.enums.UnitStatus.RENTED)
                .build();

        Agreement agreement = Agreement.builder()
                .id(UUID.randomUUID())
                .agreementNumber("AGR-TEST-01")
                .status(AgreementStatus.ACTIVE)
                .tenant(tenant)
                .property(property)
                .unit(unit)
                .build();

        when(agreementRepository.findByAgreementNumber("AGR-TEST-01")).thenReturn(Optional.of(agreement));

        agreementService.tenantCancelAgreement("AGR-TEST-01", "tenant@example.com");

        assertThat(agreement.getStatus()).isEqualTo(AgreementStatus.CANCELLED);
        assertThat(property.getStatus()).isEqualTo(PropertyStatus.LISTED);
        assertThat(unit.getStatus()).isEqualTo(com.ethiorental.backend.property.enums.UnitStatus.AVAILABLE);
        verify(agreementRepository).save(agreement);
        verify(propertyRepository).save(property);
        verify(propertyUnitRepository).save(unit);
    }
}
