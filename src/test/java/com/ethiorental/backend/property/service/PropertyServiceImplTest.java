package com.ethiorental.backend.property.service;

import com.ethiorental.backend.IAM.entity.Citizen;
import com.ethiorental.backend.IAM.repository.CitizenRepository;
import com.ethiorental.backend.IAM.repository.EmployeeCredentialRepository;
import com.ethiorental.backend.IAM.repository.EmployeeRoleRepository;
import com.ethiorental.backend.agreement.entity.Agreement;
import com.ethiorental.backend.agreement.enums.AgreementStatus;
import com.ethiorental.backend.agreement.repository.AgreementRepository;
import com.ethiorental.backend.lease.repository.LeaseRequestRepository;
import com.ethiorental.backend.property.dto.PropertyResponse;
import com.ethiorental.backend.property.dto.PropertyUnitResponse;
import com.ethiorental.backend.property.entity.Property;
import com.ethiorental.backend.property.entity.PropertyUnit;
import com.ethiorental.backend.property.enums.PropertyStatus;
import com.ethiorental.backend.property.enums.UnitStatus;
import com.ethiorental.backend.property.exception.PropertyNotFoundException;
import com.ethiorental.backend.property.mapper.PropertyMapper;
import com.ethiorental.backend.property.repository.PropertyRepository;
import com.ethiorental.backend.property.repository.PropertyUnitRepository;
import com.ethiorental.backend.property.repository.PropertyVerificationRepository;
import com.ethiorental.backend.location.repository.SubCityWoredaRepository;
import com.ethiorental.backend.property.storage.MinioStorageService;
import com.ethiorental.backend.shared.audit.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PropertyServiceImplTest {

    @Mock private PropertyRepository propertyRepository;
    @Mock private CitizenRepository citizenRepository;
    @Mock private PropertyMapper mapper;
    @Mock private MinioStorageService storageService;
    @Mock private PropertyVerificationRepository verificationRepository;
    @Mock private EmployeeCredentialRepository employeeCredentialRepository;
    @Mock private EmployeeRoleRepository employeeRoleRepository;
    @Mock private SubCityWoredaRepository subCityWoredaRepository;
    @Mock private AuditService auditService;
    @Mock private PropertyUnitRepository propertyUnitRepository;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private AgreementRepository agreementRepository;
    @Mock private LeaseRequestRepository leaseRequestRepository;

    @InjectMocks
    private PropertyServiceImpl propertyService;

    private UUID propertyId;
    private Property shoppingMall;
    private PropertyUnit unit1;
    private PropertyUnit unit2;
    private Citizen tenant;

    @BeforeEach
    void setUp() {
        propertyId = UUID.randomUUID();
        shoppingMall = Property.builder()
                .id(propertyId)
                .propertyCode("MALL-999")
                .propertyType("Shopping Mall")
                .title("Addis Grand Mall")
                .status(PropertyStatus.LISTED)
                .build();

        unit1 = PropertyUnit.builder()
                .id(UUID.randomUUID())
                .property(shoppingMall)
                .unitCode("SHOP-101")
                .unitName("Retail Shop A")
                .unitType("Retail")
                .areaSqMeter(BigDecimal.valueOf(50))
                .status(UnitStatus.AVAILABLE) // Initially available in DB
                .rentAmount(BigDecimal.valueOf(25000))
                .build();

        unit2 = PropertyUnit.builder()
                .id(UUID.randomUUID())
                .property(shoppingMall)
                .unitCode("SHOP-102")
                .unitName("Retail Shop B")
                .unitType("Retail")
                .areaSqMeter(BigDecimal.valueOf(60))
                .status(UnitStatus.AVAILABLE)
                .rentAmount(BigDecimal.valueOf(30000))
                .build();

        tenant = Citizen.builder()
                .id(UUID.randomUUID())
                .firstName("Abebe")
                .lastName("Kebede")
                .email("abebe@example.com")
                .build();
    }

    @Test
    void getPropertyByCode_MultiUnitMall_ReconcilesRentedUnitWithActiveAgreement() {
        // unit1 has an active agreement
        Agreement agreement = Agreement.builder()
                .id(UUID.randomUUID())
                .agreementNumber("AGR-MALL-1")
                .requestCode("LR-MALL-1")
                .property(shoppingMall)
                .unit(unit1)
                .tenant(tenant)
                .status(AgreementStatus.ACTIVE)
                .monthlyRent(BigDecimal.valueOf(25000))
                .build();

        when(propertyRepository.findByPropertyCodeMatches("MALL-999"))
                .thenReturn(List.of(shoppingMall));
        when(propertyUnitRepository.findByPropertyId(propertyId))
                .thenReturn(List.of(unit1, unit2));
        when(agreementRepository.findByPropertyId(propertyId))
                .thenReturn(List.of(agreement));

        when(mapper.toPropertyResponse(any(Property.class), anyList())).thenAnswer(inv -> {
            Property p = inv.getArgument(0);
            return new PropertyResponse(
                p.getId(), p.getPropertyCode(), p.getPropertyType(), p.getTitle(),
                null, null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, p.getStatus(), null, null, null, null,
                List.of(), List.of(), List.of(), null
            );
        });

        PropertyResponse result = propertyService.getPropertyByCode("MALL-999");

        assertThat(result).isNotNull();
        // unit1 should now be RENTED and tenant set
        assertThat(unit1.getStatus()).isEqualTo(UnitStatus.RENTED);
        assertThat(unit1.getTenantName()).isEqualTo("Abebe Kebede");
        // unit2 should remain AVAILABLE
        assertThat(unit2.getStatus()).isEqualTo(UnitStatus.AVAILABLE);

        verify(propertyUnitRepository).save(unit1);
        verify(propertyUnitRepository, never()).save(unit2);
    }

    @Test
    void getPropertyByCode_SingleHouseRented_ThrowsPropertyNotFoundException() {
        Property singleHouse = Property.builder()
                .id(UUID.randomUUID())
                .propertyCode("VILLA-101")
                .propertyType("Villa")
                .title("Luxury Villa")
                .status(PropertyStatus.RENTED)
                .build();

        when(propertyRepository.findByPropertyCodeMatches("VILLA-101"))
                .thenReturn(List.of(singleHouse));
        when(propertyUnitRepository.findByPropertyId(singleHouse.getId()))
                .thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> propertyService.getPropertyByCode("VILLA-101"))
                .isInstanceOf(PropertyNotFoundException.class)
                .hasMessageContaining("currently rented");
    }

    @Test
    void getPropertyUnits_ReconcilesUnitStatusesAccurately() {
        Agreement agreement = Agreement.builder()
                .id(UUID.randomUUID())
                .agreementNumber("AGR-MALL-1")
                .requestCode("LR-MALL-1")
                .property(shoppingMall)
                .unit(unit1)
                .tenant(tenant)
                .status(AgreementStatus.ACTIVE)
                .monthlyRent(BigDecimal.valueOf(25000))
                .build();

        when(propertyRepository.findWithDetailsById(propertyId)).thenReturn(shoppingMall);
        when(propertyUnitRepository.findByPropertyId(propertyId)).thenReturn(List.of(unit1, unit2));
        when(agreementRepository.findByPropertyId(propertyId)).thenReturn(List.of(agreement));

        when(mapper.toUnitResponse(any(PropertyUnit.class))).thenAnswer(inv -> {
            PropertyUnit u = inv.getArgument(0);
            return new PropertyUnitResponse(
                u.getId(), shoppingMall.getId(), u.getUnitCode(), u.getUnitName(),
                u.getUnitType(), u.getAreaSqMeter(), u.getStatus(), u.getRentAmount(),
                u.getTenantName(), u.getFloorLevel(), u.getCategory(), u.getSubmeter(),
                u.getWaterSupply(), u.getFrontage(), u.getDescription(), "", List.of(), null, null
            );
        });

        List<PropertyUnitResponse> results = propertyService.getPropertyUnits(propertyId);

        assertThat(results).hasSize(2);
        assertThat(unit1.getStatus()).isEqualTo(UnitStatus.RENTED);
        assertThat(unit1.getTenantName()).isEqualTo("Abebe Kebede");
        assertThat(unit2.getStatus()).isEqualTo(UnitStatus.AVAILABLE);
    }
}
