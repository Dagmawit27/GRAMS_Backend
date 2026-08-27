package com.ethiorental.backend.property.service;

import com.ethiorental.backend.IAM.entity.Citizen;
import com.ethiorental.backend.IAM.entity.GovernmentEmployee;
import com.ethiorental.backend.IAM.repository.CitizenRepository;
import com.ethiorental.backend.IAM.repository.EmployeeCredentialRepository;
import com.ethiorental.backend.IAM.repository.EmployeeRoleRepository;
import com.ethiorental.backend.location.repository.SubCityWoredaRepository;
import com.ethiorental.backend.property.dto.PropertyRequest;
import com.ethiorental.backend.property.dto.PropertyResponse;
import com.ethiorental.backend.property.dto.PropertyUnitResponse;
import com.ethiorental.backend.property.entity.*;
import com.ethiorental.backend.property.enums.PropertyStatus;
import com.ethiorental.backend.property.exception.PropertyNotFoundException;
import com.ethiorental.backend.property.mapper.PropertyMapper;
import com.ethiorental.backend.property.repository.*;
import com.ethiorental.backend.property.storage.MinioStorageService;
import com.ethiorental.backend.location.repository.SubCityWoredaRepository;
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
    private final EmployeeRoleRepository employeeRoleRepository;
    private final SubCityWoredaRepository subCityWoredaRepository;
    private final AuditService auditService;
    private final PropertyUnitRepository propertyUnitRepository;

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

        // Validate that the property's sub-city + woreda is a recognised Addis Ababa jurisdiction
        String reqSubCity = request.address().subCity();
        String reqWoreda  = request.address().woreda();
        if (!subCityWoredaRepository.existsBySubCityIgnoreCaseAndWoreda(reqSubCity, reqWoreda)) {
            throw new IllegalArgumentException(
                "Invalid address: '" + reqSubCity + "' Woreda " + reqWoreda
                + " is not a recognised Addis Ababa sub-city/woreda combination."
            );
        }

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
        Property property = propertyRepository.findWithDetailsById(id);
        if (property == null) {
            throw new PropertyNotFoundException("Property not found: " + id);
        }
        return mapper.toPropertyResponse(property);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PropertyResponse> getPropertiesByStatus(PropertyStatus status) {
        return propertyRepository.findByStatus(status)
                .stream().map(mapper::toPropertyResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PropertyResponse> getPropertiesByJurisdiction(String subCity, String woreda, PropertyStatus status) {
        if (!subCityWoredaRepository.existsBySubCityIgnoreCaseAndWoreda(subCity, woreda)) {
            throw new IllegalArgumentException(
                "Invalid jurisdiction: '" + subCity + "' Woreda " + woreda + " is not recognised."
            );
        }
        return propertyRepository.findByJurisdiction(subCity, woreda, status)
                .stream().map(mapper::toPropertyResponse).toList();
    }

    // SRS §5.10, NFR-034, BR-027 — log property verification / status-change events
    @Override
    @Transactional
    public PropertyResponse updatePropertyStatus(UUID id, PropertyStatus newStatus, String remarks, String officerUsername) {
        Property property = propertyRepository.findWithDetailsById(id);
        if (property == null) {
            throw new PropertyNotFoundException("Property not found: " + id);
        }

        GovernmentEmployee officer = employeeCredentialRepository.findByEmail(officerUsername)
                .orElseThrow(() -> new IllegalArgumentException("Officer not found"))
                .getEmployee();

        // ── Role-based transition enforcement ────────────────────────────────
        // Determine the caller's roles
        List<String> callerRoles = employeeRoleRepository.findByEmployee(officer)
                .stream().map(r -> r.getRole().getRoleName().toUpperCase()).toList();

        PropertyStatus currentStatus = property.getStatus();

        if (callerRoles.contains("WOREDA_OFFICER")) {
            // Officers may only act on PENDING properties → VERIFIED or REJECTED
            if (currentStatus != PropertyStatus.PENDING) {
                throw new IllegalArgumentException(
                    "Officer can only verify PENDING properties. Current status: " + currentStatus);
            }
            if (newStatus != PropertyStatus.VERIFIED && newStatus != PropertyStatus.REJECTED) {
                throw new IllegalArgumentException(
                    "Officer can only set status to VERIFIED or REJECTED.");
            }
        } else if (callerRoles.contains("WOREDA_SUPERVISOR")) {
            // Supervisors may only act on VERIFIED properties → LISTED or REJECTED
            if (currentStatus != PropertyStatus.VERIFIED) {
                throw new IllegalArgumentException(
                    "Supervisor can only approve VERIFIED properties. Current status: " + currentStatus);
            }
            if (newStatus != PropertyStatus.LISTED && newStatus != PropertyStatus.REJECTED) {
                throw new IllegalArgumentException(
                    "Supervisor can only set status to LISTED or REJECTED.");
            }
        }
        // Higher admin roles (SUB_CITY_ADMINISTRATOR, CITY_ADMINISTRATOR, SYSTEM_ADMINISTRATOR) have no restriction

        PropertyStatus previousStatus = currentStatus;
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

    @Override
    @Transactional
    @Auditable(action = AuditAction.DELETE, module = "PROPERTY")
    public void deleteProperty(UUID id, String landlordEmail) {
        Property property = propertyRepository.findWithDetailsById(id);
        if (property == null) {
            throw new PropertyNotFoundException("Property not found: " + id);
        }

        // Verify ownership
        if (!property.getLandlord().getEmail().equals(landlordEmail)) {
            throw new IllegalArgumentException("You can only delete your own properties");
        }

        // Only allow deletion if status is PENDING
        if (property.getStatus() != PropertyStatus.PENDING) {
            throw new IllegalStateException("Only pending properties can be deleted. Current status: " + property.getStatus());
        }

        // Delete images from MinIO
        for (PropertyImage image : property.getImages()) {
            try {
                storageService.deletePropertyImage(image.getImageUrl());
            } catch (Exception e) {
                // Log but continue with deletion
                System.err.println("Failed to delete image from MinIO: " + image.getImageUrl());
            }
        }

        // Delete ownership documents from MinIO
        for (OwnershipDocument doc : property.getOwnershipDocuments()) {
            try {
                storageService.deleteOwnershipDocument(doc.getFilePath());
            } catch (Exception e) {
                // Log but continue with deletion
                System.err.println("Failed to delete document from MinIO: " + doc.getFilePath());
            }
        }

        // Delete the property (cascade will delete related entities)
        propertyRepository.delete(property);
    }

    @Override
    @Transactional
    @Auditable(action = AuditAction.UPDATE, module = "PROPERTY")
    public PropertyResponse updateProperty(UUID id, PropertyRequest request, List<MultipartFile> images, List<MultipartFile> ownershipDocuments, String landlordEmail) {
        Property property = propertyRepository.findWithDetailsById(id);
        if (property == null) {
            throw new PropertyNotFoundException("Property not found: " + id);
        }

        // Verify ownership
        if (!property.getLandlord().getEmail().equals(landlordEmail)) {
            throw new IllegalArgumentException("You can only update your own properties");
        }

        // Only allow update if status is PENDING
        if (property.getStatus() != PropertyStatus.PENDING) {
            throw new IllegalStateException("Only pending properties can be updated. Current status: " + property.getStatus());
        }

        // Update basic property fields
        Address address = mapper.toAddressEntity(request.address());
        property.setAddress(address);
        property.setPropertyType(request.propertyType());
        property.setTitle(request.title());
        property.setHouseNumber(request.houseNumber());
        property.setFloorNumber(request.floorNumber());
        property.setBedroomCount(request.bedroomCount());
        property.setBathroomCount(request.bathroomCount());
        property.setAreaSqMeter(request.areaSqMeter());
        property.setMonthlyRent(request.monthlyRent());
        property.setFurnishingStatus(request.furnishingStatus());
        property.setDescription(request.description());
        property.setOwnershipType(request.ownershipType());
        property.setSpecificLandmark(request.specificLandmark());
        property.setCadastralParcelId(request.cadastralParcelId());
        property.setTitleDeedNumber(request.titleDeedNumber());
        property.setSecurityDepositMonths(request.securityDepositMonths());
        property.setMinLeasePeriod(request.minLeasePeriod());
        property.setAvailableFrom(request.availableFrom());

        // Handle new images
        if (images != null && !images.isEmpty()) {
            // Delete existing images from MinIO
            for (PropertyImage existingImage : property.getImages()) {
                try {
                    storageService.deletePropertyImage(existingImage.getImageUrl());
                } catch (Exception e) {
                    System.err.println("Failed to delete old image from MinIO: " + existingImage.getImageUrl());
                }
            }
            property.getImages().clear();

            // Upload new images
            boolean firstIsCover = true;
            for (MultipartFile file : images) {
                if (file == null || file.isEmpty()) continue;
                String objectName = storageService.uploadPropertyImage(file, property.getId());
                PropertyImage img = PropertyImage.builder()
                        .property(property)
                        .imageUrl(objectName)
                        .isCover(firstIsCover)
                        .build();
                property.getImages().add(img);
                firstIsCover = false;
            }
        }

        // Handle new ownership documents
        if (ownershipDocuments != null && !ownershipDocuments.isEmpty()) {
            // Delete existing documents from MinIO
            for (OwnershipDocument existingDoc : property.getOwnershipDocuments()) {
                try {
                    storageService.deleteOwnershipDocument(existingDoc.getFilePath());
                } catch (Exception e) {
                    System.err.println("Failed to delete old document from MinIO: " + existingDoc.getFilePath());
                }
            }
            property.getOwnershipDocuments().clear();

            // Upload new documents
            int docIndex = 1;
            for (MultipartFile file : ownershipDocuments) {
                if (file == null || file.isEmpty()) continue;
                String objectName = storageService.uploadOwnershipDocument(file, property.getId());
                String baseDocNumber = (request.titleDeedNumber() != null && !request.titleDeedNumber().isBlank())
                        ? request.titleDeedNumber()
                        : "DOC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                String uniqueDocNumber = (docIndex == 1) ? baseDocNumber : baseDocNumber + "-" + docIndex;

                OwnershipDocument doc = OwnershipDocument.builder()
                        .property(property)
                        .documentNumber(uniqueDocNumber)
                        .documentType(detectDocumentType(file.getOriginalFilename()))
                        .filePath(objectName)
                        .build();
                property.getOwnershipDocuments().add(doc);
                docIndex++;
            }
        }

        return mapper.toPropertyResponse(propertyRepository.save(property));
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

    // ── Unit Management ───────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<PropertyUnitResponse> getPropertyUnits(UUID propertyId) {
        Property property = propertyRepository.findWithDetailsById(propertyId);
        if (property == null) {
            throw new PropertyNotFoundException("Property not found: " + propertyId);
        }
        return propertyUnitRepository.findByPropertyId(propertyId)
                .stream().map(mapper::toUnitResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PropertyUnitResponse getUnitById(UUID unitId) {
        PropertyUnit unit = propertyUnitRepository.findById(unitId)
                .orElseThrow(() -> new IllegalArgumentException("Unit not found: " + unitId));
        return mapper.toUnitResponse(unit);
    }

    @Override
    @Transactional
    public List<PropertyUnitResponse> addUnitsToProperty(UUID propertyId, List<PropertyUnitResponse> units, String landlordEmail) {
        Property property = propertyRepository.findWithDetailsById(propertyId);
        if (property == null) {
            throw new PropertyNotFoundException("Property not found: " + propertyId);
        }

        // Verify ownership
        if (!property.getLandlord().getEmail().equals(landlordEmail)) {
            throw new IllegalArgumentException("You can only add units to your own properties");
        }

        List<PropertyUnit> savedUnits = new ArrayList<>();
        for (PropertyUnitResponse unitDto : units) {
            PropertyUnit unit = PropertyUnit.builder()
                    .property(property)
                    .unitCode(unitDto.unitCode())
                    .unitName(unitDto.unitName())
                    .unitType(unitDto.unitType())
                    .areaSqMeter(unitDto.areaSqMeter())
                    .status(com.ethiorental.backend.property.enums.UnitStatus.AVAILABLE)
                    .rentAmount(unitDto.rentAmount())
                    .floorLevel(unitDto.floorLevel())
                    .category(unitDto.category())
                    .shopNumber(unitDto.shopNumber())
                    .submeter(unitDto.submeter())
                    .waterSupply(unitDto.waterSupply())
                    .frontage(unitDto.frontage())
                    .description(unitDto.description())
                    .build();
            savedUnits.add(propertyUnitRepository.save(unit));
        }

        return savedUnits.stream().map(mapper::toUnitResponse).toList();
    }

    @Override
    @Transactional
    public PropertyUnitResponse updateUnitStatus(UUID unitId, String newStatus, String tenantName, String landlordEmail) {
        PropertyUnit unit = propertyUnitRepository.findById(unitId)
                .orElseThrow(() -> new IllegalArgumentException("Unit not found: " + unitId));

        // Verify ownership
        if (!unit.getProperty().getLandlord().getEmail().equals(landlordEmail)) {
            throw new IllegalArgumentException("You can only update units in your own properties");
        }

        com.ethiorental.backend.property.enums.UnitStatus status = com.ethiorental.backend.property.enums.UnitStatus.valueOf(newStatus.toUpperCase());
        unit.setStatus(status);
        unit.setTenantName(tenantName);

        return mapper.toUnitResponse(propertyUnitRepository.save(unit));
    }

    @Override
    @Transactional
    public void deleteUnit(UUID unitId, String landlordEmail) {
        PropertyUnit unit = propertyUnitRepository.findById(unitId)
                .orElseThrow(() -> new IllegalArgumentException("Unit not found: " + unitId));

        // Verify ownership
        if (!unit.getProperty().getLandlord().getEmail().equals(landlordEmail)) {
            throw new IllegalArgumentException("You can only delete units from your own properties");
        }

        // Only allow deletion if unit is available
        if (unit.getStatus() != com.ethiorental.backend.property.enums.UnitStatus.AVAILABLE) {
            throw new IllegalStateException("Only available units can be deleted. Current status: " + unit.getStatus());
        }

        propertyUnitRepository.delete(unit);
    }
}
