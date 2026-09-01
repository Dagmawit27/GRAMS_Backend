package com.ethiorental.backend.lease.service;

import com.ethiorental.backend.IAM.entity.Citizen;
import com.ethiorental.backend.IAM.repository.CitizenRepository;
import com.ethiorental.backend.lease.dto.LeaseRequestRequest;
import com.ethiorental.backend.lease.dto.LeaseRequestResponse;
import com.ethiorental.backend.lease.dto.LeaseStatusUpdateRequest;
import com.ethiorental.backend.lease.entity.LeaseRequest;
import com.ethiorental.backend.lease.enums.LeaseRequestStatus;
import com.ethiorental.backend.lease.event.LeaseRequestSubmittedEvent;
import com.ethiorental.backend.lease.event.LeaseRequestStatusChangedEvent;
import com.ethiorental.backend.lease.event.LeaseRequestCancelledEvent;
import com.ethiorental.backend.lease.event.AgreementGeneratedEvent;
import com.ethiorental.backend.lease.repository.LeaseRequestRepository;
import com.ethiorental.backend.property.entity.Property;
import com.ethiorental.backend.property.entity.PropertyUnit;
import com.ethiorental.backend.property.enums.PropertyStatus;
import com.ethiorental.backend.property.enums.UnitStatus;
import com.ethiorental.backend.property.repository.PropertyRepository;
import com.ethiorental.backend.property.repository.PropertyUnitRepository;
import com.ethiorental.backend.property.storage.MinioStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LeaseRequestServiceImpl implements LeaseRequestService {

    private final LeaseRequestRepository leaseRequestRepository;
    private final CitizenRepository citizenRepository;
    private final PropertyRepository propertyRepository;
    private final PropertyUnitRepository propertyUnitRepository;
    private final MinioStorageService storageService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public LeaseRequestResponse submitLeaseRequest(LeaseRequestRequest request, String applicantEmail) {
        Citizen applicant = citizenRepository.findByEmail(applicantEmail)
                .orElseThrow(() -> new IllegalArgumentException("User account not found. Please ensure you are logged in with a valid account."));

        Property property = propertyRepository.findById(request.propertyId())
                .orElseThrow(() -> new IllegalArgumentException("Property not found. The property ID provided is invalid."));

        if (property.getStatus() != PropertyStatus.LISTED) {
            throw new IllegalStateException("This property is not currently available for lease. Property status: " + property.getStatus() + ". Please contact the landlord for more information.");
        }

        PropertyUnit unit = null;
        if (request.unitId() != null) {
            unit = propertyUnitRepository.findById(request.unitId())
                    .orElseThrow(() -> new IllegalArgumentException("Unit not found. The unit ID provided is invalid."));
            
            if (unit.getStatus() != UnitStatus.AVAILABLE) {
                throw new IllegalStateException("This unit is not currently available for lease. Unit status: " + unit.getStatus() + ". Please select a different unit.");
            }

            // Check if applicant already has a pending request for this unit
            leaseRequestRepository.findByUnitIdAndApplicantId(request.unitId(), applicant.getId())
                    .ifPresent(lr -> {
                        throw new IllegalStateException("You already have a pending lease request for this unit. Please wait for the landlord's response before submitting another request.");
                    });
        } else {
            // Check if applicant already has a pending request for this property
            leaseRequestRepository.findByPropertyIdAndApplicantId(request.propertyId(), applicant.getId())
                    .ifPresent(lr -> {
                        throw new IllegalStateException("You already have a pending lease request for this property. Please wait for the landlord's response before submitting another request.");
                    });
        }

        LeaseRequest leaseRequest = LeaseRequest.builder()
                .property(property)
                .unit(unit)
                .applicant(applicant)
                .landlord(property.getLandlord())
                .proposedRent(request.proposedRent())
                .leaseDurationMonths(request.leaseDurationMonths())
                .applicantNotes(request.applicantNotes())
                .status(LeaseRequestStatus.PENDING)
                .expiresAt(LocalDateTime.now().plusDays(7)) // Expires in 7 days
                .build();

        LeaseRequest saved = leaseRequestRepository.save(leaseRequest);

        // Publish LeaseRequestSubmittedEvent
        log.info("Preparing to publish LeaseRequestSubmittedEvent for requestCode: {}, landlordEmail: {}", 
                 saved.getRequestCode(), property.getLandlord().getEmail());
        LeaseRequestSubmittedEvent event = new LeaseRequestSubmittedEvent(
            this,
            saved.getId(),
            saved.getRequestCode(),
            applicant.getFirstName() + " " + applicant.getLastName(),
            applicant.getEmail(),
            applicant.getPhone(),
            property.getId(),
            property.getPropertyCode(),
            property.getTitle(),
            property.getLandlord().getId().toString(),
            property.getLandlord().getEmail(),
            property.getMonthlyRent().toString()
        );
        eventPublisher.publishEvent(event);
        log.info("LeaseRequestSubmittedEvent published for requestCode: {}", saved.getRequestCode());

        return toResponse(saved);
    }

    @Override
    public List<LeaseRequestResponse> getMyLeaseRequests(String applicantEmail) {
        Citizen applicant = citizenRepository.findByEmail(applicantEmail)
                .orElseThrow(() -> new IllegalArgumentException("User account not found. Please ensure you are logged in with a valid account."));
        
        List<LeaseRequest> requests = leaseRequestRepository.findByApplicantId(applicant.getId());
        return requests.stream().map(this::toResponse).toList();
    }

    @Override
    public List<LeaseRequestResponse> getLandlordLeaseRequests(String landlordEmail) {
        Citizen landlord = citizenRepository.findByEmail(landlordEmail)
                .orElseThrow(() -> new IllegalArgumentException("Landlord account not found. Please ensure you are logged in with a valid account."));
        
        List<LeaseRequest> requests = leaseRequestRepository.findByLandlordId(landlord.getId());
        return requests.stream().map(this::toResponse).toList();
    }

    @Override
    public LeaseRequestResponse getLeaseRequestByCode(String requestCode) {
        LeaseRequest leaseRequest = leaseRequestRepository.findByRequestCode(requestCode)
                .orElseThrow(() -> new IllegalArgumentException("Lease request not found. The request code provided is invalid."));
        return toResponse(leaseRequest);
    }

    @Override
    @Transactional
    public LeaseRequestResponse updateLeaseRequestStatus(String requestCode, LeaseStatusUpdateRequest request, String landlordEmail) {
        LeaseRequest leaseRequest = leaseRequestRepository.findByRequestCode(requestCode)
                .orElseThrow(() -> new IllegalArgumentException("Lease request not found. The request code provided is invalid."));

        Citizen landlord = citizenRepository.findByEmail(landlordEmail)
                .orElseThrow(() -> new IllegalArgumentException("Landlord account not found. Please ensure you are logged in with a valid account."));

        if (!leaseRequest.getLandlord().getId().equals(landlord.getId())) {
            throw new IllegalArgumentException("Access denied. You can only update lease requests for your own properties.");
        }

        if (leaseRequest.getStatus() != LeaseRequestStatus.PENDING) {
            throw new IllegalStateException("This lease request cannot be updated. Current status: " + leaseRequest.getStatus() + ". Only pending requests can be modified.");
        }

        LeaseRequestStatus oldStatus = leaseRequest.getStatus();
        leaseRequest.setStatus(request.newStatus());
        leaseRequest.setLandlordRemarks(request.remarks());
        leaseRequest.setReviewedAt(LocalDateTime.now());

        LeaseRequest updated = leaseRequestRepository.save(leaseRequest);

        // Publish LeaseRequestStatusChangedEvent
        LeaseRequestStatusChangedEvent event = new LeaseRequestStatusChangedEvent(
            this,
            updated.getId(),
            updated.getRequestCode(),
            oldStatus.name(),
            request.newStatus().name(),
            leaseRequest.getApplicant().getFirstName() + " " + leaseRequest.getApplicant().getLastName(),
            leaseRequest.getApplicant().getEmail(),
            leaseRequest.getProperty().getId(),
            leaseRequest.getProperty().getPropertyCode(),
            leaseRequest.getProperty().getTitle(),
            landlord.getFirstName() + " " + landlord.getLastName(),
            landlord.getEmail()
        );
        eventPublisher.publishEvent(event);
        log.info("LeaseRequestStatusChangedEvent published for requestCode: {} from {} to {}", 
                 updated.getRequestCode(), oldStatus, request.newStatus());

        return toResponse(updated);
    }

    @Override
    @Transactional
    public void cancelLeaseRequest(String requestCode, String applicantEmail) {
        LeaseRequest leaseRequest = leaseRequestRepository.findByRequestCode(requestCode)
                .orElseThrow(() -> new IllegalArgumentException("Lease request not found. The request code provided is invalid."));

        Citizen applicant = citizenRepository.findByEmail(applicantEmail)
                .orElseThrow(() -> new IllegalArgumentException("User account not found. Please ensure you are logged in with a valid account."));

        if (!leaseRequest.getApplicant().getId().equals(applicant.getId())) {
            throw new IllegalArgumentException("Access denied. You can only cancel your own lease requests.");
        }

        if (leaseRequest.getStatus() != LeaseRequestStatus.PENDING) {
            throw new IllegalStateException("This lease request cannot be cancelled. Current status: " + leaseRequest.getStatus() + ". Only pending requests can be cancelled.");
        }

        leaseRequest.setStatus(LeaseRequestStatus.CANCELLED);
        LeaseRequest saved = leaseRequestRepository.save(leaseRequest);

        // Publish LeaseRequestCancelledEvent
        log.info("Preparing to publish LeaseRequestCancelledEvent for requestCode: {}, landlordEmail: {}", 
                 saved.getRequestCode(), saved.getLandlord().getEmail());
        LeaseRequestCancelledEvent event = new LeaseRequestCancelledEvent(
            this,
            saved.getId(),
            saved.getRequestCode(),
            applicant.getFirstName() + " " + applicant.getLastName(),
            applicant.getEmail(),
            saved.getProperty().getId(),
            saved.getProperty().getPropertyCode(),
            saved.getProperty().getTitle(),
            saved.getLandlord().getId().toString(),
            saved.getLandlord().getEmail()
        );
        eventPublisher.publishEvent(event);
        log.info("LeaseRequestCancelledEvent published for requestCode: {}", saved.getRequestCode());
    }

    @Override
    @Transactional
    public void deleteLeaseRequest(String requestCode, String applicantEmail) {
        LeaseRequest leaseRequest = leaseRequestRepository.findByRequestCode(requestCode)
                .orElseThrow(() -> new IllegalArgumentException("Lease request not found. The request code provided is invalid."));

        Citizen applicant = citizenRepository.findByEmail(applicantEmail)
                .orElseThrow(() -> new IllegalArgumentException("User account not found. Please ensure you are logged in with a valid account."));

        if (!leaseRequest.getApplicant().getId().equals(applicant.getId())) {
            throw new IllegalArgumentException("Access denied. You can only delete your own lease requests.");
        }

        if (leaseRequest.getStatus() != LeaseRequestStatus.CANCELLED) {
            throw new IllegalStateException("This lease request cannot be deleted. Current status: " + leaseRequest.getStatus() + ". Only cancelled requests can be deleted.");
        }

        leaseRequestRepository.delete(leaseRequest);
        log.info("Lease request deleted: {}", requestCode);
    }

    @Override
    public List<LeaseRequestResponse> getPendingRequestsForProperty(UUID propertyId, String landlordEmail) {
        Citizen landlord = citizenRepository.findByEmail(landlordEmail)
                .orElseThrow(() -> new IllegalArgumentException("Landlord account not found. Please ensure you are logged in with a valid account."));

        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new IllegalArgumentException("Property not found. The property ID provided is invalid."));

        if (!property.getLandlord().getId().equals(landlord.getId())) {
            throw new IllegalArgumentException("Access denied. You can only view requests for your own properties.");
        }

        List<LeaseRequest> requests = leaseRequestRepository.findByPropertyId(propertyId).stream()
                .filter(lr -> lr.getStatus() == LeaseRequestStatus.PENDING)
                .toList();
        
        return requests.stream().map(this::toResponse).toList();
    }

    @Override
    public List<LeaseRequestResponse> getPendingRequestsForUnit(UUID unitId, String landlordEmail) {
        Citizen landlord = citizenRepository.findByEmail(landlordEmail)
                .orElseThrow(() -> new IllegalArgumentException("Landlord account not found. Please ensure you are logged in with a valid account."));

        PropertyUnit unit = propertyUnitRepository.findById(unitId)
                .orElseThrow(() -> new IllegalArgumentException("Unit not found. The unit ID provided is invalid."));

        if (!unit.getProperty().getLandlord().getId().equals(landlord.getId())) {
            throw new IllegalArgumentException("Access denied. You can only view requests for your own properties.");
        }

        List<LeaseRequest> requests = leaseRequestRepository.findByUnitId(unitId).stream()
                .filter(lr -> lr.getStatus() == LeaseRequestStatus.PENDING)
                .toList();
        
        return requests.stream().map(this::toResponse).toList();
    }

    private LeaseRequestResponse toResponse(LeaseRequest leaseRequest) {
        Property property = leaseRequest.getProperty();
        PropertyUnit unit = leaseRequest.getUnit();
        
        // Get property images and resolve MinIO URLs
        List<String> propertyImages = property.getImages() != null 
            ? property.getImages().stream().map(img -> storageService.resolveImageUrl(img.getImageUrl())).toList()
            : List.of();
        
        String propertyImage = propertyImages.isEmpty() ? "" : propertyImages.get(0);
        
        // Calculate security deposit (2 months rent)
        java.math.BigDecimal securityDeposit = leaseRequest.getProposedRent().multiply(java.math.BigDecimal.valueOf(2));
        
        // Calculate start and end dates
        String startDate = leaseRequest.getCreatedAt().toLocalDate().toString();
        String endDate = leaseRequest.getCreatedAt().plusMonths(leaseRequest.getLeaseDurationMonths()).toLocalDate().toString();
        
        // Get area as BigDecimal
        java.math.BigDecimal area = unit != null ? unit.getAreaSqMeter() : 
            (property.getAreaSqMeter() != null ? property.getAreaSqMeter() : java.math.BigDecimal.ZERO);
        
        return new LeaseRequestResponse(
                leaseRequest.getRequestCode(),
                leaseRequest.getRequestCode(),
                property.getPropertyCode(),
                property.getTitle(),
                property.getPropertyType(),
                property.getAddress() != null ? property.getAddress().getCity() + ", " + property.getAddress().getSubCity() : "",
                propertyImage,
                propertyImages,
                unit != null ? unit.getUnitCode() : null,
                unit != null ? unit.getUnitName() : null,
                area,
                leaseRequest.getApplicant().getFirstName() + " " + leaseRequest.getApplicant().getLastName(),
                leaseRequest.getApplicant().getEmail(),
                leaseRequest.getApplicant().getPhone(),
                leaseRequest.getApplicant().getNationalId(),
                leaseRequest.getApplicant().getWorksOn(),
                leaseRequest.getLandlord().getFirstName() + " " + leaseRequest.getLandlord().getLastName(),
                leaseRequest.getLandlord().getEmail(),
                leaseRequest.getProposedRent(),
                securityDeposit,
                leaseRequest.getLeaseDurationMonths(),
                startDate,
                endDate,
                leaseRequest.getApplicantNotes(),
                leaseRequest.getLandlordRemarks(),
                leaseRequest.getStatus(),
                leaseRequest.getCreatedAt(),
                leaseRequest.getReviewedAt(),
                leaseRequest.getExpiresAt()
        );
    }

}
