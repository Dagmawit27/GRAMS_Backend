package com.ethiorental.backend.agreement.service;

import com.ethiorental.backend.agreement.dto.AgreementResponse;
import com.ethiorental.backend.agreement.entity.Agreement;
import com.ethiorental.backend.agreement.enums.AgreementStatus;
import com.ethiorental.backend.agreement.repository.AgreementRepository;
import com.ethiorental.backend.lease.entity.LeaseRequest;
import com.ethiorental.backend.property.entity.PropertyUnit;
import com.ethiorental.backend.property.enums.PropertyStatus;
import com.ethiorental.backend.property.enums.UnitStatus;
import com.ethiorental.backend.property.repository.PropertyRepository;
import com.ethiorental.backend.property.repository.PropertyUnitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.ethiorental.backend.tax.service.TaxService;

@Service
@RequiredArgsConstructor
@Slf4j
public class AgreementServiceImpl implements AgreementService {

    private final AgreementRepository agreementRepository;
    private final PropertyRepository propertyRepository;
    private final PropertyUnitRepository propertyUnitRepository;
    private final TaxService taxService;

    @Override
    @Transactional
    public AgreementResponse createAgreementFromLeaseRequest(LeaseRequest leaseRequest, String supervisorEmail) {
        log.info("Creating formal Agreement from LeaseRequest: {}", leaseRequest.getRequestCode());

        // Check if agreement already exists for this requestCode
        Optional<Agreement> existing = agreementRepository.findByRequestCode(leaseRequest.getRequestCode());
        if (existing.isPresent()) {
            log.info("Agreement already exists for request code {}: {}", leaseRequest.getRequestCode(), existing.get().getAgreementNumber());
            return toResponse(existing.get());
        }

        int durationMonths = leaseRequest.getLeaseDurationMonths() != null ? leaseRequest.getLeaseDurationMonths() : 12;
        BigDecimal monthlyRent = leaseRequest.getProposedRent() != null ? leaseRequest.getProposedRent() : BigDecimal.ZERO;
        BigDecimal securityDeposit = monthlyRent.multiply(BigDecimal.valueOf(2));

        String agreementNumber = "AGR" + System.currentTimeMillis() + (int)(Math.random() * 1000);
        LocalDateTime now = LocalDateTime.now();
        int dueDay = now.getDayOfMonth();

        Agreement agreement = Agreement.builder()
                .agreementNumber(agreementNumber)
                .requestCode(leaseRequest.getRequestCode())
                .leaseRequestId(leaseRequest.getId())
                .property(leaseRequest.getProperty())
                .unit(leaseRequest.getUnit())
                .tenant(leaseRequest.getApplicant())
                .landlord(leaseRequest.getLandlord())
                .monthlyRent(monthlyRent)
                .securityDeposit(securityDeposit)
                .advancePaymentMonths(2)
                .leaseDurationMonths(durationMonths)
                .contractDate(now.toLocalDate())
                .startDate(now)
                .endDate(now.plusMonths(durationMonths))
                .monthlyPaymentDueDay(dueDay)
                .totalMonthsPaid(0)
                .paidThroughDate(null)
                .nextPaymentDueDate(now.toLocalDate())
                .utilitiesPaidBy("TENANT")
                .propertyCondition("GOOD")
                .propertyOwnershipType("PRIVATE")
                .status(AgreementStatus.ACTIVE)
                .landlordSigned(Boolean.TRUE.equals(leaseRequest.getLandlordSigned()))
                .landlordSignedAt(leaseRequest.getLandlordSignedAt() != null ? leaseRequest.getLandlordSignedAt() : LocalDateTime.now())
                .landlordSignature("SEALED:" + (leaseRequest.getLandlord() != null ? leaseRequest.getLandlord().getEmail() : "LANDLORD"))
                .tenantSigned(Boolean.TRUE.equals(leaseRequest.getTenantSigned()))
                .tenantSignedAt(leaseRequest.getTenantSignedAt() != null ? leaseRequest.getTenantSignedAt() : LocalDateTime.now())
                .tenantSignature("SEALED:" + (leaseRequest.getApplicant() != null ? leaseRequest.getApplicant().getEmail() : "TENANT"))
                .officerVerified(true)
                .officerVerifiedAt(leaseRequest.getReviewedAt() != null ? leaseRequest.getReviewedAt() : LocalDateTime.now())
                .officerEmail("woreda-officer")
                .supervisorApproved(true)
                .supervisorApprovedAt(LocalDateTime.now())
                .supervisorEmail(supervisorEmail)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Agreement saved = agreementRepository.save(agreement);

        // Update Property and Unit status
        if (leaseRequest.getProperty() != null) {
            try {
                if (leaseRequest.getUnit() != null) {
                    PropertyUnit unit = leaseRequest.getUnit();
                    unit.setStatus(UnitStatus.RENTED);
                    if (leaseRequest.getApplicant() != null) {
                        String applicantName = ((leaseRequest.getApplicant().getFirstName() != null ? leaseRequest.getApplicant().getFirstName() : "") + " " +
                                (leaseRequest.getApplicant().getLastName() != null ? leaseRequest.getApplicant().getLastName() : "")).trim();
                        unit.setTenantName(applicantName);
                    }
                    propertyUnitRepository.save(unit);
                    log.info("Marked unit {} as RENTED for agreement {}", unit.getUnitCode(), saved.getAgreementNumber());

                    // Check if there are other available units in this building
                    List<PropertyUnit> allUnits = propertyUnitRepository.findByPropertyId(leaseRequest.getProperty().getId());
                    boolean hasAvailableUnits = allUnits.stream().anyMatch(u -> u.getStatus() == UnitStatus.AVAILABLE);

                    if (!hasAvailableUnits) {
                        leaseRequest.getProperty().setStatus(PropertyStatus.RENTED);
                        propertyRepository.save(leaseRequest.getProperty());
                        log.info("All units rented for property {}. Marked building as RENTED.", leaseRequest.getProperty().getPropertyCode());
                    } else {
                        leaseRequest.getProperty().setStatus(PropertyStatus.LISTED);
                        propertyRepository.save(leaseRequest.getProperty());
                        log.info("Unit {} rented. Building {} still has available units, keeping property LISTED.",
                                unit.getUnitCode(), leaseRequest.getProperty().getPropertyCode());
                    }
                } else {
                    // Single house without units
                    leaseRequest.getProperty().setStatus(PropertyStatus.RENTED);
                    propertyRepository.save(leaseRequest.getProperty());
                    log.info("Marked single property {} as RENTED", leaseRequest.getProperty().getPropertyCode());
                }
            } catch (Exception e) {
                log.warn("Failed to update property/unit status: {}", e.getMessage(), e);
            }
        }

        log.info("Successfully created Agreement {} for requestCode {}", saved.getAgreementNumber(), saved.getRequestCode());

        // Synchronize LandlordTax record for all active agreements of this landlord
        try {
            taxService.onAgreementActivated(saved);
        } catch (Exception ex) {
            log.warn("Failed to sync tax ledger for active agreement {}: {}", saved.getAgreementNumber(), ex.getMessage());
        }

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AgreementResponse> getMyAgreements(String userEmail) {
        List<Agreement> agreements = agreementRepository.findByLandlordEmailOrTenantEmail(userEmail);
        return agreements.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AgreementResponse> getLandlordAgreements(String userEmail) {
        List<Agreement> agreements = agreementRepository.findByLandlordEmail(userEmail);
        return agreements.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AgreementResponse> getTenantAgreements(String userEmail) {
        List<Agreement> agreements = agreementRepository.findByTenantEmail(userEmail);
        return agreements.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AgreementResponse getAgreementByNumber(String agreementNumber) {
        Agreement agreement = agreementRepository.findByAgreementNumber(agreementNumber)
                .orElseThrow(() -> new IllegalArgumentException("Agreement not found with number: " + agreementNumber));
        return toResponse(agreement);
    }

    @Override
    @Transactional(readOnly = true)
    public AgreementResponse getAgreementByRequestCode(String requestCode) {
        Agreement agreement = agreementRepository.findByRequestCode(requestCode)
                .orElseThrow(() -> new IllegalArgumentException("Agreement not found with request code: " + requestCode));
        return toResponse(agreement);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AgreementResponse> getAllActiveAgreements() {
        List<Agreement> agreements = agreementRepository.findByStatusOrderByCreatedAtDesc(AgreementStatus.ACTIVE);
        return agreements.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AgreementResponse> getAllAgreements() {
        List<Agreement> agreements = agreementRepository.findAllByOrderByCreatedAtDesc();
        return agreements.stream().map(this::toResponse).toList();
    }

    private AgreementResponse toResponse(Agreement a) {
        String propSubCity = (a.getProperty() != null && a.getProperty().getAddress() != null)
                ? a.getProperty().getAddress().getSubCity() : "";
        String propWoreda = (a.getProperty() != null && a.getProperty().getAddress() != null)
                ? a.getProperty().getAddress().getWoreda() : "";

        String tenantName = a.getTenant() != null
                ? (a.getTenant().getFirstName() + " " + a.getTenant().getMiddleName()).trim() : "";
        String landlordName = a.getLandlord() != null
                ? (a.getLandlord().getFirstName() + " " + a.getLandlord().getMiddleName()).trim() : "";

        return AgreementResponse.builder()
                .id(a.getId())
                .agreementNumber(a.getAgreementNumber())
                .requestCode(a.getRequestCode())
                .leaseRequestId(a.getLeaseRequestId())
                // Property details
                .propertyId(a.getProperty() != null ? a.getProperty().getId() : null)
                .propertyCode(a.getProperty() != null ? a.getProperty().getPropertyCode() : "")
                .propertyTitle(a.getProperty() != null ? a.getProperty().getTitle() : "")
                .propertyType(a.getProperty() != null ? a.getProperty().getPropertyType() : "")
                .propertySubCity(propSubCity)
                .propertyWoreda(propWoreda)
                .unitId(a.getUnit() != null ? a.getUnit().getId() : null)
                .unitCode(a.getUnit() != null ? a.getUnit().getUnitCode() : "")
                .unitNumber(a.getUnit() != null ? a.getUnit().getUnitName() : "")
                // Tenant details
                .tenantId(a.getTenant() != null ? a.getTenant().getId() : null)
                .tenantName(tenantName)
                .tenantEmail(a.getTenant() != null ? a.getTenant().getEmail() : "")
                .tenantPhone(a.getTenant() != null ? a.getTenant().getPhone() : "")
                .tenantSubCity(a.getTenant() != null ? a.getTenant().getSubCity() : "")
                .tenantWoreda(a.getTenant() != null ? a.getTenant().getWoreda() : "")
                // Landlord details
                .landlordId(a.getLandlord() != null ? a.getLandlord().getId() : null)
                .landlordName(landlordName)
                .landlordEmail(a.getLandlord() != null ? a.getLandlord().getEmail() : "")
                .landlordPhone(a.getLandlord() != null ? a.getLandlord().getPhone() : "")
                .landlordSubCity(a.getLandlord() != null ? a.getLandlord().getSubCity() : "")
                .landlordWoreda(a.getLandlord() != null ? a.getLandlord().getWoreda() : "")
                .landlordPreferredPaymentMethod(a.getLandlord() != null ? a.getLandlord().getPreferredPaymentMethod() : "")
                .landlordBankName(a.getLandlord() != null ? a.getLandlord().getBankName() : "")
                .landlordAccountNumber(a.getLandlord() != null ? a.getLandlord().getAccountNumber() : "")
                .landlordAccountHolderName(a.getLandlord() != null ? a.getLandlord().getAccountHolderName() : "")
                .landlordBankName2(a.getLandlord() != null ? a.getLandlord().getBankName2() : "")
                .landlordAccountNumber2(a.getLandlord() != null ? a.getLandlord().getAccountNumber2() : "")
                .landlordAccountHolderName2(a.getLandlord() != null ? a.getLandlord().getAccountHolderName2() : "")
                .landlordBankName3(a.getLandlord() != null ? a.getLandlord().getBankName3() : "")
                .landlordAccountNumber3(a.getLandlord() != null ? a.getLandlord().getAccountNumber3() : "")
                .landlordAccountHolderName3(a.getLandlord() != null ? a.getLandlord().getAccountHolderName3() : "")
                .landlordTinNumber(a.getLandlord() != null ? a.getLandlord().getTinNumber() : "")
                // Terms
                .monthlyRent(a.getMonthlyRent())
                .securityDeposit(a.getSecurityDeposit())
                .advancePaymentMonths(a.getAdvancePaymentMonths())
                .leaseDurationMonths(a.getLeaseDurationMonths())
                .contractDate(a.getContractDate())
                .startDate(a.getStartDate())
                .endDate(a.getEndDate())
                .monthlyPaymentDueDay(a.getMonthlyPaymentDueDay())
                .utilitiesPaidBy(a.getUtilitiesPaidBy())
                .propertyCondition(a.getPropertyCondition())
                .propertyOwnershipType(a.getPropertyOwnershipType())
                .status(a.getStatus() != null ? a.getStatus().name() : "ACTIVE")
                .totalMonthsPaid(a.getTotalMonthsPaid() != null ? a.getTotalMonthsPaid() : 0)
                .paidThroughDate(a.getPaidThroughDate())
                .nextPaymentDueDate(a.getNextPaymentDueDate() != null ? a.getNextPaymentDueDate() : (a.getStartDate() != null ? a.getStartDate().toLocalDate() : null))
                .cancellationRequestedAt(a.getCancellationRequestedAt())
                .cancellationRequestedByLandlord(a.getCancellationRequestedByLandlord())
                // Signatures
                .landlordSigned(a.getLandlordSigned())
                .landlordSignedAt(a.getLandlordSignedAt())
                .landlordSignature(a.getLandlordSignature())
                .tenantSigned(a.getTenantSigned())
                .tenantSignedAt(a.getTenantSignedAt())
                .tenantSignature(a.getTenantSignature())
                .officerVerified(a.getOfficerVerified())
                .officerVerifiedAt(a.getOfficerVerifiedAt())
                .officerEmail(a.getOfficerEmail())
                .supervisorApproved(a.getSupervisorApproved())
                .supervisorApprovedAt(a.getSupervisorApprovedAt())
                .supervisorEmail(a.getSupervisorEmail())
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional
    public void expireAgreement(UUID agreementId) {
        Agreement agreement = agreementRepository.findById(agreementId)
                .orElseThrow(() -> new IllegalArgumentException("Agreement not found"));
        agreement.setStatus(AgreementStatus.EXPIRED);
        
        // Release property back to LISTED
        if (agreement.getProperty() != null) {
            agreement.getProperty().setStatus(PropertyStatus.LISTED);
            propertyRepository.save(agreement.getProperty());
        }
        // Release unit back to AVAILABLE
        if (agreement.getUnit() != null) {
            agreement.getUnit().setStatus(UnitStatus.AVAILABLE);
            propertyUnitRepository.save(agreement.getUnit());
        }
        agreementRepository.save(agreement);
        log.info("Agreement {} expired. Property released.", agreement.getAgreementNumber());
        try {
            taxService.onAgreementActivated(agreement);
        } catch (Exception ex) {
            log.warn("Failed to sync tax ledger after expiring agreement: {}", ex.getMessage());
        }
    }

    @Override
    @Transactional
    public void renewAgreement(String agreementNumber, String userEmail) {
        Agreement agreement = agreementRepository.findByAgreementNumber(agreementNumber)
                .orElseThrow(() -> new IllegalArgumentException("Agreement not found: " + agreementNumber));
        
        if (agreement.getStatus() != AgreementStatus.ACTIVE) {
            throw new IllegalStateException("Only active agreements can be renewed. Current status: " + agreement.getStatus());
        }
        
        // Extend by 24 months (2 years) from current end date
        LocalDateTime currentEnd = agreement.getEndDate() != null ? agreement.getEndDate() : LocalDateTime.now();
        LocalDateTime newEndDate = currentEnd.plusMonths(24);
        agreement.setEndDate(newEndDate);
        agreement.setLeaseDurationMonths((agreement.getLeaseDurationMonths() != null ? agreement.getLeaseDurationMonths() : 0) + 24);
        agreementRepository.save(agreement);
        log.info("Agreement {} renewed until {}. Requested by {}", agreementNumber, newEndDate, userEmail);
        try {
            taxService.onAgreementActivated(agreement);
        } catch (Exception ex) {
            log.warn("Failed to sync tax ledger after renewing agreement: {}", ex.getMessage());
        }
    }

    @Override
    @Transactional
    public void requestCancellation(String agreementNumber, String landlordEmail) {
        Agreement agreement = agreementRepository.findByAgreementNumber(agreementNumber)
                .orElseThrow(() -> new IllegalArgumentException("Agreement not found: " + agreementNumber));
        
        // Verify the requester is the landlord
        if (agreement.getLandlord() == null || !agreement.getLandlord().getEmail().equalsIgnoreCase(landlordEmail)) {
            throw new IllegalArgumentException("Only the landlord can request cancellation.");
        }
        
        if (agreement.getStatus() != AgreementStatus.ACTIVE) {
            throw new IllegalStateException("Only active agreements can be cancelled. Current status: " + agreement.getStatus());
        }
        
        agreement.setStatus(AgreementStatus.CANCELLATION_REQUESTED);
        agreement.setCancellationRequestedAt(LocalDateTime.now());
        agreement.setCancellationRequestedByLandlord(true);
        agreementRepository.save(agreement);
        log.info("Landlord {} requested cancellation of agreement {}", landlordEmail, agreementNumber);
    }

    @Override
    @Transactional
    public void acceptCancellation(String agreementNumber, String tenantEmail) {
        Agreement agreement = agreementRepository.findByAgreementNumber(agreementNumber)
                .orElseThrow(() -> new IllegalArgumentException("Agreement not found: " + agreementNumber));
        
        // If not system auto-cancel, verify the requester is the tenant
        if (!"system".equalsIgnoreCase(tenantEmail)) {
            if (agreement.getTenant() == null || !agreement.getTenant().getEmail().equalsIgnoreCase(tenantEmail)) {
                throw new IllegalArgumentException("Only the tenant can accept cancellation.");
            }
        }
        
        if (agreement.getStatus() != AgreementStatus.CANCELLATION_REQUESTED) {
            throw new IllegalStateException("No cancellation request pending. Current status: " + agreement.getStatus());
        }
        
        agreement.setStatus(AgreementStatus.CANCELLED);
        
        // Release property back to LISTED
        if (agreement.getProperty() != null) {
            agreement.getProperty().setStatus(PropertyStatus.LISTED);
            propertyRepository.save(agreement.getProperty());
        }
        if (agreement.getUnit() != null) {
            agreement.getUnit().setStatus(UnitStatus.AVAILABLE);
            propertyUnitRepository.save(agreement.getUnit());
        }
        agreementRepository.save(agreement);
        log.info("Agreement {} cancelled. Property released.", agreementNumber);
        try {
            taxService.onAgreementActivated(agreement);
        } catch (Exception ex) {
            log.warn("Failed to sync tax ledger after cancelling agreement: {}", ex.getMessage());
        }
    }

    @Override
    @Transactional
    public void tenantCancelAgreement(String agreementNumber, String tenantEmail) {
        Agreement agreement = agreementRepository.findByAgreementNumber(agreementNumber)
                .orElseThrow(() -> new IllegalArgumentException("Agreement not found: " + agreementNumber));

        if (agreement.getTenant() == null || !agreement.getTenant().getEmail().equalsIgnoreCase(tenantEmail)) {
            throw new IllegalArgumentException("Only the tenant can cancel this agreement.");
        }

        if (agreement.getStatus() != AgreementStatus.ACTIVE && agreement.getStatus() != AgreementStatus.CANCELLATION_REQUESTED) {
            throw new IllegalStateException("Only active agreements can be cancelled. Current status: " + agreement.getStatus());
        }

        agreement.setStatus(AgreementStatus.CANCELLED);

        // Release property back to LISTED
        if (agreement.getProperty() != null) {
            agreement.getProperty().setStatus(PropertyStatus.LISTED);
            propertyRepository.save(agreement.getProperty());
        }
        if (agreement.getUnit() != null) {
            agreement.getUnit().setStatus(UnitStatus.AVAILABLE);
            propertyUnitRepository.save(agreement.getUnit());
        }
        agreementRepository.save(agreement);
        log.info("Tenant {} cancelled agreement {}. Property released.", tenantEmail, agreementNumber);
        try {
            taxService.onAgreementActivated(agreement);
        } catch (Exception ex) {
            log.warn("Failed to sync tax ledger after tenant cancelling agreement: {}", ex.getMessage());
        }
    }
}
