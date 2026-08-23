package com.ethiorental.backend.property.service;

import com.ethiorental.backend.IAM.entity.Citizen;
import com.ethiorental.backend.IAM.entity.GovernmentEmployee;
import com.ethiorental.backend.IAM.repository.CitizenRepository;
import com.ethiorental.backend.IAM.repository.EmployeeCredentialRepository;
import com.ethiorental.backend.property.dto.PropertyRequest;
import com.ethiorental.backend.property.dto.PropertyResponse;
import com.ethiorental.backend.property.entity.*;
import com.ethiorental.backend.property.enums.PropertyStatus;
import com.ethiorental.backend.property.exception.PropertyNotFoundException;
import com.ethiorental.backend.property.mapper.PropertyMapper;
import com.ethiorental.backend.property.repository.*;
import com.ethiorental.backend.property.storage.MinioStorageService;
import com.ethiorental.backend.shared.audit.AuditAction;
import com.ethiorental.backend.shared.audit.AuditService;
import com.ethiorental.backend.shared.audit.Auditable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@Service
@RequiredArgsConstructor
public class PropertyServiceImpl implements PropertyService {

    private final PropertyRepository propertyRepository;
    private final CitizenRepository citizenRepository;
    private final PropertyMapper mapper;
    private final MinioStorageService storageService;
    private final PropertyVerificationRepository verificationRepository;
    private final EmployeeCredentialRepository employeeCredentialRepository;
    private final AuditService auditService;

    // ── Register ──────────────────────────────────────────────────────────────

    // SRS §5.10, NFR-034, BR-027 — log all property creation events
    @Override
    @Auditable(action = AuditAction.CREATE, module = "PROPERTY")
    @Transactional
    public PropertyResponse registerProperty(PropertyRequest request,
                                              List<MultipartFile> images,
                                              List<MultipartFile> ownershipDocuments,
                                              String landlordEmail) {

        Citizen landlord = citizenRepository.findByEmail(landlordEmail)
                .orElseThrow(() -> new IllegalArgumentException("Landlord account not found for email: " + landlordEmail));

        Address address = mapper.toAddressEntity(request.address());

        Property property = Property.builder()
                .landlord(landlord)
                .address(address)
                .propertyCode(generatePropertyCode())
                .propertyType(request.propertyType())
                .title(request.title())
                .houseNumber(request.houseNumber())
                .floorNumber(request.floorNumber())
                .bedroomCount(request.bedroomCount())
                .bathroomCount(request.bathroomCount())
                .areaSqMeter(request.areaSqMeter())
                .monthlyRent(request.monthlyRent())
                .furnishingStatus(request.furnishingStatus())
                .description(request.description())
                .ownershipType(request.ownershipType())
                .specificLandmark(request.specificLandmark())
                .cadastralParcelId(request.cadastralParcelId())
                .titleDeedNumber(request.titleDeedNumber())
                .securityDepositMonths(request.securityDepositMonths())
                .minLeasePeriod(request.minLeasePeriod())
                .availableFrom(request.availableFrom())
                .status(PropertyStatus.PENDING)
                .images(new ArrayList<>())
                .ownershipDocuments(new ArrayList<>())
                .build();

        // Save first to get the generated id for MinIO paths
        Property saved = propertyRepository.save(property);

        // Upload images to MinIO
        if (images != null && !images.isEmpty()) {
            boolean firstIsCover = true;
            for (MultipartFile file : images) {
                if (file == null || file.isEmpty()) continue;
                String objectName = storageService.uploadPropertyImage(file, saved.getId());
                PropertyImage img = PropertyImage.builder()
                        .property(saved)
                        .imageUrl(objectName)
                        .isCover(firstIsCover)
                        .build();
                saved.getImages().add(img);
                firstIsCover = false;
            }
        }

        // Upload ownership documents to MinIO
        if (ownershipDocuments != null && !ownershipDocuments.isEmpty()) {
            int docIndex = 1;
            for (MultipartFile file : ownershipDocuments) {
                if (file == null || file.isEmpty()) continue;
                String objectName = storageService.uploadOwnershipDocument(file, saved.getId());
                String baseDocNumber = (request.titleDeedNumber() != null && !request.titleDeedNumber().isBlank())
                        ? request.titleDeedNumber()
                        : "DOC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                String uniqueDocNumber = (docIndex == 1) ? baseDocNumber : baseDocNumber + "-" + docIndex;

                OwnershipDocument doc = OwnershipDocument.builder()
                        .property(saved)
                        .documentNumber(uniqueDocNumber)
                        .documentType(detectDocumentType(file.getOriginalFilename()))
                        .filePath(objectName)
                        .build();
                saved.getOwnershipDocuments().add(doc);
                docIndex++;
            }
        }

        return mapper.toPropertyResponse(propertyRepository.save(saved));
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<PropertyResponse> getMyProperties(String landlordEmail) {
        Citizen landlord = citizenRepository.findByEmail(landlordEmail)
                .orElseThrow(() -> new IllegalArgumentException("Landlord not found"));
        return propertyRepository.findByLandlord(landlord)
                .stream().map(mapper::toPropertyResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PropertyResponse getPropertyById(UUID id) {
        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new PropertyNotFoundException("Property not found: " + id));
        return mapper.toPropertyResponse(property);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PropertyResponse> getPropertiesByStatus(PropertyStatus status) {
        return propertyRepository.findByStatus(status)
                .stream().map(mapper::toPropertyResponse).toList();
    }

    // SRS §5.10, NFR-034, BR-027 — log property verification / status-change events
    @Override
    @Transactional
    public PropertyResponse updatePropertyStatus(UUID id, PropertyStatus newStatus, String remarks, String officerUsername) {
        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new PropertyNotFoundException("Property not found: " + id));

        GovernmentEmployee officer = employeeCredentialRepository.findByEmail(officerUsername)
                .orElseThrow(() -> new IllegalArgumentException("Officer not found"))
                .getEmployee();

        PropertyStatus previousStatus = property.getStatus();
        property.setStatus(newStatus);
        Property saved = propertyRepository.save(property);

        PropertyVerification verification = PropertyVerification.builder()
                .property(saved)
                .verifiedBy(officer)
                .office(officer.getOffice())
                .verificationStatus(newStatus.name())
                .remarks(remarks)
                .build();
        verificationRepository.save(verification);

        // Determine correct audit action based on the new status
        AuditAction action = switch (newStatus) {
            case VERIFIED, LISTED -> AuditAction.VERIFY;
            case REJECTED -> AuditAction.REJECT;
            case RENTED -> AuditAction.APPROVE;
            default -> AuditAction.UPDATE;
        };

        // SRS NFR-034 — record the status transition in the immutable audit log
        auditService.logStatusChange(
                "PROPERTY",
                saved.getId().toString(),
                previousStatus.name(),
                newStatus.name(),
                action,
                remarks
        );

        return mapper.toPropertyResponse(saved);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String generatePropertyCode() {
        return "PROP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private String detectDocumentType(String filename) {
        if (filename == null) return "DOCUMENT";
        String lower = filename.toLowerCase();
        if (lower.endsWith(".pdf")) return "PDF";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "JPEG";
        if (lower.endsWith(".png")) return "PNG";
        return "DOCUMENT";
    }
}
