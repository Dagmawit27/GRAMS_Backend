package com.ethiorental.backend.property.service;

import com.ethiorental.backend.IAM.entity.Citizen;
import com.ethiorental.backend.IAM.entity.GovernmentEmployee;
import com.ethiorental.backend.IAM.repository.CitizenRepository;
import com.ethiorental.backend.IAM.repository.EmployeeCredentialRepository;
import com.ethiorental.backend.IAM.repository.EmployeeRoleRepository;
import com.ethiorental.backend.agreement.entity.Agreement;
import com.ethiorental.backend.agreement.enums.AgreementStatus;
import com.ethiorental.backend.agreement.repository.AgreementRepository;
import com.ethiorental.backend.location.repository.SubCityWoredaRepository;
import com.ethiorental.backend.lease.entity.LeaseRequest;
import com.ethiorental.backend.lease.enums.LeaseRequestStatus;
import com.ethiorental.backend.lease.repository.LeaseRequestRepository;
import com.ethiorental.backend.property.dto.PropertyRequest;
import com.ethiorental.backend.property.dto.PropertyResponse;
import com.ethiorental.backend.property.dto.PropertyUnitRequest;
import com.ethiorental.backend.property.dto.PropertyUnitResponse;
import com.ethiorental.backend.property.entity.*;
import com.ethiorental.backend.property.enums.PropertyStatus;
import com.ethiorental.backend.property.enums.UnitStatus;
import com.ethiorental.backend.property.event.PropertyRegisteredEvent;
import com.ethiorental.backend.property.event.PropertyVerifiedEvent;
import com.ethiorental.backend.property.event.PropertyDeletedEvent;
import com.ethiorental.backend.property.event.PropertyApprovedEvent;
import com.ethiorental.backend.property.exception.PropertyNotFoundException;
import com.ethiorental.backend.property.mapper.PropertyMapper;
import com.ethiorental.backend.property.repository.*;
import com.ethiorental.backend.property.storage.MinioStorageService;
import com.ethiorental.backend.shared.audit.AuditAction;
import com.ethiorental.backend.shared.audit.AuditService;
import com.ethiorental.backend.shared.audit.Auditable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@Slf4j
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
    private final ApplicationEventPublisher eventPublisher;
    private final AgreementRepository agreementRepository;
    private final LeaseRequestRepository leaseRequestRepository;

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
                .units(new ArrayList<>())
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

        // Process property units (for shopping malls)
        if (request.units() != null && !request.units().isEmpty()) {
            for (PropertyUnitRequest unitRequest : request.units()) {
                PropertyUnit unit = PropertyUnit.builder()
                        .property(saved)
                        .unitCode(generateUnitCode())
                        .unitName(unitRequest.unitName())
                        .unitType(unitRequest.unitType())
                        .areaSqMeter(unitRequest.areaSqMeter())
                        .status(unitRequest.status() != null ?
                            UnitStatus.valueOf(unitRequest.status().toUpperCase()) : UnitStatus.AVAILABLE)
                        .rentAmount(unitRequest.rentAmount())
                        .tenantName(unitRequest.tenantName())
                        .floorLevel(unitRequest.floorLevel())
                        .category(unitRequest.category())
                        .submeter(unitRequest.submeter() != null ? unitRequest.submeter() : false)
                        .waterSupply(unitRequest.waterSupply() != null ? unitRequest.waterSupply() : false)
                        .frontage(unitRequest.frontage())
                        .description(unitRequest.description())
                        .build();
                saved.getUnits().add(unit);
            }
        }

        Property finalProperty = propertyRepository.save(saved);

        // Publish PropertyRegisteredEvent for notification system
        log.info("Publishing PropertyRegisteredEvent for propertyCode: {}, subCity: {}, woreda: {}", 
            finalProperty.getPropertyCode(), 
            finalProperty.getAddress().getSubCity(), 
            finalProperty.getAddress().getWoreda());
        
        PropertyRegisteredEvent event = new PropertyRegisteredEvent(
            this,
            finalProperty.getId(),
            finalProperty.getPropertyCode(),
            finalProperty.getTitle(),
            finalProperty.getPropertyType(),
            finalProperty.getAddress().getCity(),
            finalProperty.getAddress().getSubCity(),
            finalProperty.getAddress().getWoreda(),
            landlord.getId().toString(),
            landlord.getFirstName() + " " + landlord.getLastName()
        );
        eventPublisher.publishEvent(event);
        log.info("PropertyRegisteredEvent published successfully");

        return mapper.toPropertyResponse(finalProperty);
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public List<PropertyResponse> getMyProperties(String landlordEmail) {
        Citizen landlord = citizenRepository.findByEmail(landlordEmail)
                .orElseThrow(() -> new IllegalArgumentException("Landlord not found"));
        List<Property> properties = propertyRepository.findByLandlord(landlord);
        return properties.stream().map(p -> {
            List<PropertyUnit> units = getAndSyncPropertyUnits(p);
            return mapper.toPropertyResponse(p, units);
        }).toList();
    }

    @Override
    @Transactional
    public PropertyResponse getPropertyById(UUID id) {
        Property property = propertyRepository.findWithDetailsById(id);
        if (property == null) {
            throw new PropertyNotFoundException("Property not found: " + id);
        }
        List<PropertyUnit> units = getAndSyncPropertyUnits(property);
        return mapper.toPropertyResponse(property, units);
    }

    @Override
    @Transactional
    public PropertyResponse getPropertyByCode(String propertyCode) {
        if (propertyCode == null || propertyCode.trim().isEmpty()) {
            throw new PropertyNotFoundException("Property code must not be empty");
        }
        String cleanCode = propertyCode.trim();

        // 1. Look up property by property code (case-insensitive and trimmed)
        List<Property> matched = propertyRepository.findByPropertyCodeMatches(cleanCode);
        Property property = !matched.isEmpty() ? matched.get(0) : null;

        // 2. Fallback: try finding by UUID if cleanCode is a valid UUID
        if (property == null) {
            try {
                UUID id = UUID.fromString(cleanCode);
                property = propertyRepository.findWithDetailsById(id);
            } catch (Exception ignored) {}
        }

        if (property == null) {
            throw new PropertyNotFoundException("Property not found with code: " + propertyCode);
        }

        // 3. Load all units for this property and reconcile status with active agreements
        List<PropertyUnit> units = getAndSyncPropertyUnits(property);
        boolean hasUnits = units != null && !units.isEmpty();
        boolean isCommercialOrMall = property.getPropertyType() != null &&
                (property.getPropertyType().equalsIgnoreCase("Shopping Mall") ||
                 property.getPropertyType().equalsIgnoreCase("Commercial") ||
                 property.getPropertyType().toLowerCase().contains("mall") ||
                 property.getPropertyType().toLowerCase().contains("plaza") ||
                 property.getPropertyType().toLowerCase().contains("commercial"));
        boolean isMultiUnit = hasUnits || isCommercialOrMall;

        if (isMultiUnit) {
            // Multi-unit property (e.g. Shopping Mall / Commercial Plaza / Apartment Complex)
            // MUST ALWAYS be searchable and returnable so tenants can view the building and its units!
            // Do NOT throw 404 even if units are rented; frontend renders individual unit statuses and availability badges.
            // Do NOT overwrite database status during a read-only search operation.
            if (property.getStatus() != PropertyStatus.LISTED && property.getStatus() != PropertyStatus.RENTED) {
                throw new PropertyNotFoundException("Property not available for public viewing with code: " + propertyCode);
            }

            return mapper.toPropertyResponse(property, units);
        } else {
            // Single house property (no sub-units, e.g. Villa, single house)
            // If rented, it must not be search-displayable for new lease applications
            if (property.getStatus() == PropertyStatus.RENTED) {
                throw new PropertyNotFoundException("This property is currently rented and not available for new lease applications.");
            }
            if (property.getStatus() != PropertyStatus.LISTED) {
                throw new PropertyNotFoundException("Property not available for public viewing with code: " + propertyCode);
            }
            return mapper.toPropertyResponse(property, units);
        }
    }

    @Override
    @Transactional
    public List<PropertyResponse> getPropertiesByStatus(PropertyStatus status) {
        List<Property> properties = propertyRepository.findByStatus(status);
        return properties.stream().map(p -> {
            List<PropertyUnit> units = getAndSyncPropertyUnits(p);
            return mapper.toPropertyResponse(p, units);
        }).toList();
    }

    @Override
    @Transactional
    public List<PropertyResponse> getPropertiesByJurisdiction(String subCity, String woreda, PropertyStatus status) {
        if (!subCityWoredaRepository.existsBySubCityIgnoreCaseAndWoreda(subCity, woreda)) {
            throw new IllegalArgumentException(
                "Invalid jurisdiction: '" + subCity + "' Woreda " + woreda + " is not recognised."
            );
        }
        List<Property> properties = propertyRepository.findByJurisdiction(subCity, woreda, status);
        return properties.stream().map(p -> {
            List<PropertyUnit> units = getAndSyncPropertyUnits(p);
            return mapper.toPropertyResponse(p, units);
        }).toList();
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

        // Publish PropertyVerifiedEvent when woreda_officer verifies property
        if (newStatus == PropertyStatus.VERIFIED && callerRoles.contains("WOREDA_OFFICER")) {
            PropertyVerifiedEvent event = new PropertyVerifiedEvent(
                this,
                saved.getId(),
                saved.getPropertyCode(),
                saved.getTitle(),
                saved.getPropertyType(),
                saved.getAddress().getCity(),
                saved.getAddress().getSubCity(),
                saved.getAddress().getWoreda(),
                officer.getId().toString(),
                officer.getFirstName() + " " + officer.getLastName(),
                remarks,
                saved.getLandlord().getId().toString(),
                saved.getLandlord().getEmail()
            );
            eventPublisher.publishEvent(event);
        }

        // Publish PropertyApprovedEvent when woreda_supervisor approves property (LISTED)
        if (newStatus == PropertyStatus.LISTED && callerRoles.contains("WOREDA_SUPERVISOR")) {
            log.info("Preparing to publish PropertyApprovedEvent for propertyCode: {}, landlordEmail: {}", 
                     saved.getPropertyCode(), saved.getLandlord().getEmail());
            PropertyApprovedEvent event = new PropertyApprovedEvent(
                this,
                saved.getId(),
                saved.getPropertyCode(),
                saved.getTitle(),
                saved.getPropertyType(),
                saved.getAddress().getCity(),
                saved.getAddress().getSubCity(),
                saved.getAddress().getWoreda(),
                officer.getId().toString(),
                officer.getFirstName() + " " + officer.getLastName(),
                saved.getLandlord().getId().toString(),
                saved.getLandlord().getEmail()
            );
            eventPublisher.publishEvent(event);
            log.info("PropertyApprovedEvent published for propertyCode: {}", saved.getPropertyCode());
        }

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
        // if (property.getStatus() != PropertyStatus.PENDING) {
        //     throw new IllegalStateException("Only pending properties can be deleted. Current status: " + property.getStatus());
        // }

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

        // Publish PropertyDeletedEvent before deletion
        PropertyDeletedEvent event = new PropertyDeletedEvent(
            this,
            property.getId(),
            property.getPropertyCode(),
            property.getTitle(),
            property.getAddress().getCity(),
            property.getAddress().getSubCity(),
            property.getAddress().getWoreda()
        );
        eventPublisher.publishEvent(event);
        log.info("PropertyDeletedEvent published for propertyCode: {}", property.getPropertyCode());

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

    private String generateUnitCode() {
        return "UNIT-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }

    private String detectDocumentType(String filename) {
        if (filename == null) return "DOCUMENT";
        String lower = filename.toLowerCase();
        if (lower.endsWith(".pdf")) return "PDF";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "JPEG";
        if (lower.endsWith(".png")) return "PNG";
        return "DOCUMENT";
    }

    /**
     * Retrieves all units for a property and dynamically reconciles their status
     * against active agreements and approved lease requests.
     * If a unit has an active agreement or approved lease, its status is ensured to be RENTED
     * and tenant name populated, persisting any missing updates in PostgreSQL.
     */
    private List<PropertyUnit> getAndSyncPropertyUnits(Property property) {
        if (property == null || property.getId() == null) {
            return Collections.emptyList();
        }

        List<PropertyUnit> units = propertyUnitRepository.findByPropertyId(property.getId());
        if (units == null || units.isEmpty()) {
            return Collections.emptyList();
        }

        try {
            // 1. Collect all active agreements for this property
            List<Agreement> agreements = agreementRepository.findByPropertyId(property.getId());
            Map<UUID, String> rentedUnitIdToTenantName = new HashMap<>();
            Set<UUID> rentedUnitIds = new HashSet<>();

            if (agreements != null) {
                for (Agreement a : agreements) {
                    if (a.getStatus() != null &&
                        (a.getStatus() == AgreementStatus.TERMINATED ||
                         a.getStatus() == AgreementStatus.EXPIRED ||
                         a.getStatus() == AgreementStatus.CANCELLED)) {
                        continue;
                    }

                    String tenantName = null;
                    if (a.getTenant() != null) {
                        String first = a.getTenant().getFirstName() != null ? a.getTenant().getFirstName() : "";
                        String last = a.getTenant().getLastName() != null ? a.getTenant().getLastName() : "";
                        tenantName = (first + " " + last).trim();
                    }

                    if (a.getUnit() != null && a.getUnit().getId() != null) {
                        rentedUnitIds.add(a.getUnit().getId());
                        if (tenantName != null && !tenantName.isBlank()) {
                            rentedUnitIdToTenantName.put(a.getUnit().getId(), tenantName);
                        }
                    } else if (a.getRequestCode() != null && !a.getRequestCode().isBlank()) {
                        // Attempt to link via LeaseRequest
                        try {
                            Optional<LeaseRequest> optLr = leaseRequestRepository.findByRequestCodeWithDetails(a.getRequestCode());
                            if (optLr.isPresent() && optLr.get().getUnit() != null) {
                                PropertyUnit linkedUnit = optLr.get().getUnit();
                                rentedUnitIds.add(linkedUnit.getId());
                                if ((tenantName == null || tenantName.isBlank()) && optLr.get().getApplicant() != null) {
                                    String first = optLr.get().getApplicant().getFirstName() != null ? optLr.get().getApplicant().getFirstName() : "";
                                    String last = optLr.get().getApplicant().getLastName() != null ? optLr.get().getApplicant().getLastName() : "";
                                    tenantName = (first + " " + last).trim();
                                }
                                if (tenantName != null && !tenantName.isBlank()) {
                                    rentedUnitIdToTenantName.put(linkedUnit.getId(), tenantName);
                                }
                                a.setUnit(linkedUnit);
                                agreementRepository.save(a);
                            }
                        } catch (Exception e) {
                            log.warn("Could not lookup lease request for agreement {}: {}", a.getAgreementNumber(), e.getMessage());
                        }
                    }
                }
            }

            // 2. Also check approved / under-verification lease requests with units
            try {
                List<LeaseRequest> leaseRequests = leaseRequestRepository.findByPropertyIdWithUnit(property.getId());
                if (leaseRequests != null) {
                    for (LeaseRequest lr : leaseRequests) {
                        if (lr.getUnit() != null && lr.getUnit().getId() != null) {
                            if (lr.getStatus() == LeaseRequestStatus.LANDLORD_APPROVED ||
                                lr.getStatus() == LeaseRequestStatus.SUPERVISOR_APPROVED ||
                                lr.getStatus() == LeaseRequestStatus.UNDER_VERIFICATION ||
                                lr.getStatus() == LeaseRequestStatus.PENDING_SUPERVISOR_APPROVAL) {
                                rentedUnitIds.add(lr.getUnit().getId());
                                if (!rentedUnitIdToTenantName.containsKey(lr.getUnit().getId()) && lr.getApplicant() != null) {
                                    String first = lr.getApplicant().getFirstName() != null ? lr.getApplicant().getFirstName() : "";
                                    String last = lr.getApplicant().getLastName() != null ? lr.getApplicant().getLastName() : "";
                                    String applicantName = (first + " " + last).trim();
                                    if (!applicantName.isBlank()) {
                                        rentedUnitIdToTenantName.put(lr.getUnit().getId(), applicantName);
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Could not check lease requests for property {}: {}", property.getId(), e.getMessage());
            }

            // 3. Fallback: If an active agreement exists for this property but no unit was explicitly linked,
            // match by rent amount or associate if only one unit exists
            if (agreements != null && !agreements.isEmpty() && rentedUnitIds.isEmpty()) {
                for (Agreement a : agreements) {
                    if (a.getStatus() != null &&
                        (a.getStatus() == AgreementStatus.TERMINATED ||
                         a.getStatus() == AgreementStatus.EXPIRED ||
                         a.getStatus() == AgreementStatus.CANCELLED)) {
                        continue;
                    }
                    if (a.getMonthlyRent() != null) {
                        List<PropertyUnit> matchingUnits = units.stream()
                                .filter(u -> u.getRentAmount() != null && u.getRentAmount().compareTo(a.getMonthlyRent()) == 0)
                                .toList();
                        if (matchingUnits.size() == 1) {
                            PropertyUnit matched = matchingUnits.get(0);
                            rentedUnitIds.add(matched.getId());
                            if (a.getTenant() != null) {
                                String first = a.getTenant().getFirstName() != null ? a.getTenant().getFirstName() : "";
                                String last = a.getTenant().getLastName() != null ? a.getTenant().getLastName() : "";
                                String tName = (first + " " + last).trim();
                                rentedUnitIdToTenantName.put(matched.getId(), tName);
                            }
                            a.setUnit(matched);
                            agreementRepository.save(a);
                            break;
                        }
                    }
                    if (rentedUnitIds.isEmpty() && units.size() == 1) {
                        PropertyUnit onlyUnit = units.get(0);
                        rentedUnitIds.add(onlyUnit.getId());
                        if (a.getTenant() != null) {
                            String first = a.getTenant().getFirstName() != null ? a.getTenant().getFirstName() : "";
                            String last = a.getTenant().getLastName() != null ? a.getTenant().getLastName() : "";
                            rentedUnitIdToTenantName.put(onlyUnit.getId(), (first + " " + last).trim());
                        }
                        a.setUnit(onlyUnit);
                        agreementRepository.save(a);
                        break;
                    }
                }
            }

            // 4. Update units in-memory and persist to DB if status changed
            for (PropertyUnit unit : units) {
                if (rentedUnitIds.contains(unit.getId())) {
                    boolean modified = false;
                    if (unit.getStatus() != UnitStatus.RENTED) {
                        unit.setStatus(UnitStatus.RENTED);
                        modified = true;
                    }
                    String tName = rentedUnitIdToTenantName.get(unit.getId());
                    if (tName != null && !tName.isBlank() && !tName.equals(unit.getTenantName())) {
                        unit.setTenantName(tName);
                        modified = true;
                    }
                    if (modified) {
                        try {
                            propertyUnitRepository.save(unit);
                            log.info("Synced unit {} ({}) status to RENTED for property {}",
                                    unit.getUnitCode(), unit.getUnitName(), property.getPropertyCode());
                        } catch (Exception e) {
                            log.warn("Could not persist unit status update for {}: {}", unit.getId(), e.getMessage());
                        }
                    }
                }
            }

            // 5. Auto-reconcile parent property status if appropriate:
            // If any units are available and property was marked RENTED, restore to LISTED so it is searchable
            boolean anyAvailable = units.stream().anyMatch(u -> u.getStatus() == UnitStatus.AVAILABLE);
            if (anyAvailable && property.getStatus() == PropertyStatus.RENTED) {
                property.setStatus(PropertyStatus.LISTED);
                propertyRepository.save(property);
                log.info("Auto-corrected multi-unit property {} to LISTED because units remain available.", property.getPropertyCode());
            } else if (!anyAvailable && property.getStatus() == PropertyStatus.LISTED) {
                property.setStatus(PropertyStatus.RENTED);
                propertyRepository.save(property);
                log.info("All units rented for property {}. Marked building as RENTED.", property.getPropertyCode());
            }

        } catch (Exception ex) {
            log.warn("Error synchronizing property unit statuses for property {}: {}", property.getId(), ex.getMessage());
        }

        return units;
    }

    // ── Unit Management ───────────────────────────────────────────────────────────

    @Override
    @Transactional
    public List<PropertyUnitResponse> getPropertyUnits(UUID propertyId) {
        Property property = propertyRepository.findWithDetailsById(propertyId);
        if (property == null) {
            throw new PropertyNotFoundException("Property not found: " + propertyId);
        }
        return getAndSyncPropertyUnits(property)
                .stream().map(mapper::toUnitResponse).toList();
    }

    @Override
    @Transactional
    public PropertyUnitResponse getUnitById(UUID unitId) {
        PropertyUnit unit = propertyUnitRepository.findById(unitId)
                .orElseThrow(() -> new IllegalArgumentException("Unit not found: " + unitId));
        if (unit.getProperty() != null) {
            getAndSyncPropertyUnits(unit.getProperty());
            unit = propertyUnitRepository.findById(unitId)
                    .orElseThrow(() -> new IllegalArgumentException("Unit not found: " + unitId));
        }
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
