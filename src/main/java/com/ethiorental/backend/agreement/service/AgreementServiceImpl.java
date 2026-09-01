package com.ethiorental.backend.agreement.service;

import com.ethiorental.backend.IAM.entity.Citizen;
import com.ethiorental.backend.IAM.repository.CitizenRepository;
import com.ethiorental.backend.agreement.dto.AgreementResponse;
import com.ethiorental.backend.agreement.entity.Agreement;
import com.ethiorental.backend.agreement.repository.AgreementRepository;
import com.ethiorental.backend.lease.entity.LeaseRequest;
import com.ethiorental.backend.lease.enums.LeaseRequestStatus;
import com.ethiorental.backend.lease.repository.LeaseRequestRepository;
import com.ethiorental.backend.property.entity.Property;
import com.ethiorental.backend.property.repository.PropertyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class AgreementServiceImpl implements AgreementService {

    private final AgreementRepository agreementRepository;
    private final LeaseRequestRepository leaseRequestRepository;
    private final CitizenRepository citizenRepository;
    private final PropertyRepository propertyRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    @Transactional
    public AgreementResponse generateAgreement(String requestCode, String landlordEmail) {
        LeaseRequest leaseRequest = leaseRequestRepository.findByRequestCode(requestCode)
                .orElseThrow(() -> new IllegalArgumentException("Lease request not found. The request code provided is invalid."));

        Citizen landlord = citizenRepository.findByEmail(landlordEmail)
                .orElseThrow(() -> new IllegalArgumentException("User account not found. Please ensure you are logged in with a valid account."));

        if (!leaseRequest.getLandlord().getId().equals(landlord.getId())) {
            throw new IllegalArgumentException("Access denied. You can only generate agreements for your own properties.");
        }

        if (leaseRequest.getStatus() != LeaseRequestStatus.APPROVED) {
            throw new IllegalStateException("Agreement can only be generated for approved requests. Current status: " + leaseRequest.getStatus());
        }

        if (agreementRepository.existsByRequestCode(requestCode)) {
            throw new IllegalStateException("Agreement has already been generated for this request.");
        }

        // Generate agreement code
        String agreementCode = "AGR-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 10000);

        // Calculate advance payment (2 months rent)
        BigDecimal advancePayment = leaseRequest.getProposedRent().multiply(BigDecimal.valueOf(2));
        String advancePaymentWords = convertNumberToWords(advancePayment.longValue());

        // Build agreement
        Agreement agreement = Agreement.builder()
                .agreementCode(agreementCode)
                .contractDate(LocalDateTime.now().format(DATE_FORMATTER))
                .contractNumber("AA-HD-" + LocalDateTime.now().getYear() + "-" + (int)(Math.random() * 100000))
                .leaseRequest(leaseRequest)
                .requestCode(requestCode)
                .landlord(landlord)
                .landlordName(landlord.getFullName())
                .landlordSubCity(landlord.getSubCity() != null ? landlord.getSubCity() : "N/A")
                .landlordWoreda(landlord.getWoreda() != null ? landlord.getWoreda() : "N/A")
                .landlordHouseNo(landlord.getHouseNumber() != null ? landlord.getHouseNumber() : "N/A")
                .landlordPhone(landlord.getPhoneNumber() != null ? landlord.getPhoneNumber() : "N/A")
                .landlordRegion("Addis Ababa")
                .landlordCity(landlord.getCity() != null ? landlord.getCity() : "Addis Ababa")
                .landlordSpecificPlace(landlord.getSpecificPlace() != null ? landlord.getSpecificPlace() : "N/A")
                .tenant(leaseRequest.getApplicant())
                .tenantName(leaseRequest.getApplicant().getFullName())
                .tenantSubCity(leaseRequest.getApplicant().getSubCity() != null ? leaseRequest.getApplicant().getSubCity() : "N/A")
                .tenantWoreda(leaseRequest.getApplicant().getWoreda() != null ? leaseRequest.getApplicant().getWoreda() : "N/A")
                .tenantHouseNo(leaseRequest.getApplicant().getHouseNumber() != null ? leaseRequest.getApplicant().getHouseNumber() : "N/A")
                .tenantPhone(leaseRequest.getApplicant().getPhoneNumber() != null ? leaseRequest.getApplicant().getPhoneNumber() : "N/A")
                .tenantRegion("Addis Ababa")
                .tenantCity(leaseRequest.getApplicant().getCity() != null ? leaseRequest.getApplicant().getCity() : "Addis Ababa")
                .tenantSpecificPlace(leaseRequest.getApplicant().getSpecificPlace() != null ? leaseRequest.getApplicant().getSpecificPlace() : "N/A")
                .property(leaseRequest.getProperty())
                .propertyRegion("Addis Ababa")
                .propertyCity(leaseRequest.getProperty().getAddress() != null ? leaseRequest.getProperty().getAddress().getCity() : "Addis Ababa")
                .propertySubCity(leaseRequest.getProperty().getAddress() != null ? leaseRequest.getProperty().getAddress().getSubCity() : "N/A")
                .propertyWoreda(leaseRequest.getProperty().getAddress() != null ? leaseRequest.getProperty().getAddress().getWoreda() : "N/A")
                .propertySpecificPlace(leaseRequest.getProperty().getSpecificLandmark() != null ? leaseRequest.getProperty().getSpecificLandmark() : "N/A")
                .propertyHouseNo(leaseRequest.getProperty().getHouseNumber() != null ? leaseRequest.getProperty().getHouseNumber() : "N/A")
                .propertyOwnershipType(leaseRequest.getProperty().getPropertyType() != null ? leaseRequest.getProperty().getPropertyType() : "የራሱን የቻለ ግቢ")
                .propertyCondition("ነባር የኪራይ መኖሪያ ቤት")
                .monthlyRentInBirr(leaseRequest.getProposedRent())
                .monthlyRentInWords(convertNumberToWords(leaseRequest.getProposedRent().longValue()))
                .utilitiesPaidBy("ተከራይ")
                .advancePaymentMonths("ሁለት")
                .advancePaymentBirr(advancePayment)
                .advancePaymentWords(advancePaymentWords)
                .monthlyPaymentDueDay("5")
                .landlordSigned(false)
                .tenantSigned(false)
                .build();

        Agreement saved = agreementRepository.save(agreement);
        
        // Publish event for agreement generation
        // Note: This should be moved to a proper event publisher in the agreement module
        // For now, we'll just log it
        log.info("Agreement generated for lease request: {}", requestCode);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public AgreementResponse signAgreement(String requestCode, String otp, String landlordEmail) {
        Agreement agreement = agreementRepository.findByRequestCode(requestCode)
                .orElseThrow(() -> new IllegalArgumentException("Agreement not found for request code: " + requestCode));

        Citizen landlord = citizenRepository.findByEmail(landlordEmail)
                .orElseThrow(() -> new IllegalArgumentException("User account not found."));

        if (!agreement.getLandlord().getId().equals(landlord.getId())) {
            throw new IllegalArgumentException("Access denied. You can only sign your own agreements.");
        }

        if (agreement.isLandlordSigned()) {
            throw new IllegalStateException("You have already signed this agreement.");
        }

        // TODO: Implement OTP verification logic
        if (otp == null || otp.length() < 4) {
            throw new IllegalArgumentException("Invalid OTP. Please enter a valid OTP code.");
        }

        agreement.setLandlordSignature(landlord.getFullName());
        agreement.setLandlordSignedAt(LocalDateTime.now());
        agreement.setLandlordSigned(true);
        Agreement saved = agreementRepository.save(agreement);

        log.info("Landlord signed agreement for request: {}", requestCode);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public AgreementResponse signAgreementByTenant(String requestCode, String otp, String tenantEmail) {
        Agreement agreement = agreementRepository.findByRequestCode(requestCode)
                .orElseThrow(() -> new IllegalArgumentException("Agreement not found for request code: " + requestCode));

        Citizen tenant = citizenRepository.findByEmail(tenantEmail)
                .orElseThrow(() -> new IllegalArgumentException("User account not found."));

        if (!agreement.getTenant().getId().equals(tenant.getId())) {
            throw new IllegalArgumentException("Access denied. You can only sign your own agreements.");
        }

        if (agreement.isTenantSigned()) {
            throw new IllegalStateException("You have already signed this agreement.");
        }

        // TODO: Implement OTP verification logic
        if (otp == null || otp.length() < 4) {
            throw new IllegalArgumentException("Invalid OTP. Please enter a valid OTP code.");
        }

        agreement.setTenantSignature(tenant.getFullName());
        agreement.setTenantSignedAt(LocalDateTime.now());
        agreement.setTenantSigned(true);
        Agreement saved = agreementRepository.save(agreement);

        log.info("Tenant signed agreement for request: {}", requestCode);
        return mapToResponse(saved);
    }

    @Override
    public AgreementResponse getAgreementByRequestCode(String requestCode) {
        Agreement agreement = agreementRepository.findByRequestCode(requestCode)
                .orElseThrow(() -> new IllegalArgumentException("No agreement found for lease request code: " + requestCode + ". Please ensure the landlord has generated the agreement first."));
        return mapToResponse(agreement);
    }

    @Override
    public AgreementResponse getAgreementByAgreementCode(String agreementCode) {
        Agreement agreement = agreementRepository.findByAgreementCode(agreementCode)
                .orElseThrow(() -> new IllegalArgumentException("Agreement not found with code: " + agreementCode));
        return mapToResponse(agreement);
    }

    private AgreementResponse mapToResponse(Agreement agreement) {
        return new AgreementResponse(
                agreement.getId(),
                agreement.getAgreementCode(),
                agreement.getRequestCode(),
                agreement.getContractDate(),
                agreement.getContractNumber(),
                agreement.getLandlordName(),
                agreement.getLandlordSubCity(),
                agreement.getLandlordWoreda(),
                agreement.getLandlordHouseNo(),
                agreement.getLandlordPhone(),
                agreement.getLandlordRegion(),
                agreement.getLandlordCity(),
                agreement.getLandlordSpecificPlace(),
                agreement.getTenantName(),
                agreement.getTenantSubCity(),
                agreement.getTenantWoreda(),
                agreement.getTenantHouseNo(),
                agreement.getTenantPhone(),
                agreement.getTenantRegion(),
                agreement.getTenantCity(),
                agreement.getTenantSpecificPlace(),
                agreement.getPropertyRegion(),
                agreement.getPropertyCity(),
                agreement.getPropertySubCity(),
                agreement.getPropertyWoreda(),
                agreement.getPropertySpecificPlace(),
                agreement.getPropertyHouseNo(),
                agreement.getPropertyOwnershipType(),
                agreement.getPropertyCondition(),
                agreement.getMonthlyRentInBirr(),
                agreement.getMonthlyRentInWords(),
                agreement.getUtilitiesPaidBy(),
                agreement.getAdvancePaymentMonths(),
                agreement.getAdvancePaymentBirr(),
                agreement.getAdvancePaymentWords(),
                agreement.getMonthlyPaymentDueDay(),
                agreement.getLandlordSignature(),
                agreement.getLandlordSignedAt(),
                agreement.getTenantSignature(),
                agreement.getTenantSignedAt(),
                agreement.getOfficerName(),
                agreement.getOfficerSignature(),
                agreement.getOfficerSignedAt(),
                agreement.getWitness1Name(),
                agreement.getWitness1Signature(),
                agreement.getWitness1SignedAt(),
                agreement.getWitness2Name(),
                agreement.getWitness2Signature(),
                agreement.getWitness2SignedAt(),
                agreement.isLandlordSigned(),
                agreement.isTenantSigned(),
                agreement.getCreatedAt(),
                agreement.getUpdatedAt()
        );
    }

    // Simple number to words converter (can be enhanced with a proper library)
    private String convertNumberToWords(long number) {
        // This is a placeholder - in production, use a proper number-to-words library
        // For now, return the number as string
        return String.valueOf(number);
    }
}
