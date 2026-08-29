package com.ethiorental.backend.lease.service;

import com.ethiorental.backend.IAM.entity.Citizen;
import com.ethiorental.backend.IAM.repository.CitizenRepository;
import com.ethiorental.backend.lease.dto.LeaseRequestRequest;
import com.ethiorental.backend.lease.dto.LeaseRequestResponse;
import com.ethiorental.backend.lease.dto.LeaseStatusUpdateRequest;
import com.ethiorental.backend.lease.entity.LeaseRequest;
import com.ethiorental.backend.lease.enums.LeaseRequestStatus;
import com.ethiorental.backend.lease.repository.LeaseRequestRepository;
import com.ethiorental.backend.property.entity.Property;
import com.ethiorental.backend.property.entity.PropertyUnit;
import com.ethiorental.backend.property.enums.PropertyStatus;
import com.ethiorental.backend.property.enums.UnitStatus;
import com.ethiorental.backend.property.repository.PropertyRepository;
import com.ethiorental.backend.property.repository.PropertyUnitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LeaseRequestServiceImpl implements LeaseRequestService {

    private final LeaseRequestRepository leaseRequestRepository;
    private final CitizenRepository citizenRepository;
    private final PropertyRepository propertyRepository;
    private final PropertyUnitRepository propertyUnitRepository;

    @Override
    @Transactional
    public LeaseRequestResponse submitLeaseRequest(LeaseRequestRequest request, String applicantEmail) {
        Citizen applicant = citizenRepository.findByEmail(applicantEmail)
                .orElseThrow(() -> new IllegalArgumentException("Applicant account not found for email: " + applicantEmail));

        Property property = propertyRepository.findById(request.propertyId())
                .orElseThrow(() -> new IllegalArgumentException("Property not found: " + request.propertyId()));

        if (property.getStatus() != PropertyStatus.LISTED) {
            throw new IllegalStateException("Property is not available for lease. Current status: " + property.getStatus());
        }

        PropertyUnit unit = null;
        if (request.unitId() != null) {
            unit = propertyUnitRepository.findById(request.unitId())
                    .orElseThrow(() -> new IllegalArgumentException("Unit not found: " + request.unitId()));
            
            if (unit.getStatus() != UnitStatus.AVAILABLE) {
                throw new IllegalStateException("Unit is not available for lease. Current status: " + unit.getStatus());
            }

            // Check if applicant already has a pending request for this unit
            leaseRequestRepository.findByUnitIdAndApplicantId(request.unitId(), applicant.getId())
                    .ifPresent(lr -> {
                        throw new IllegalStateException("You already have a pending lease request for this unit");
                    });
        } else {
            // Check if applicant already has a pending request for this property
            leaseRequestRepository.findByPropertyIdAndApplicantId(request.propertyId(), applicant.getId())
                    .ifPresent(lr -> {
                        throw new IllegalStateException("You already have a pending lease request for this property");
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
        return toResponse(saved);
    }

    @Override
    public List<LeaseRequestResponse> getMyLeaseRequests(String applicantEmail) {
        Citizen applicant = citizenRepository.findByEmail(applicantEmail)
                .orElseThrow(() -> new IllegalArgumentException("Applicant account not found for email: " + applicantEmail));
        
        List<LeaseRequest> requests = leaseRequestRepository.findByApplicantId(applicant.getId());
        return requests.stream().map(this::toResponse).toList();
    }

    @Override
    public List<LeaseRequestResponse> getLandlordLeaseRequests(String landlordEmail) {
        Citizen landlord = citizenRepository.findByEmail(landlordEmail)
                .orElseThrow(() -> new IllegalArgumentException("Landlord account not found for email: " + landlordEmail));
        
        List<LeaseRequest> requests = leaseRequestRepository.findByLandlordId(landlord.getId());
        return requests.stream().map(this::toResponse).toList();
    }

    @Override
    public LeaseRequestResponse getLeaseRequestById(UUID id) {
        LeaseRequest leaseRequest = leaseRequestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Lease request not found: " + id));
        return toResponse(leaseRequest);
    }

    @Override
    @Transactional
    public LeaseRequestResponse updateLeaseRequestStatus(UUID id, LeaseStatusUpdateRequest request, String landlordEmail) {
        LeaseRequest leaseRequest = leaseRequestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Lease request not found: " + id));

        Citizen landlord = citizenRepository.findByEmail(landlordEmail)
                .orElseThrow(() -> new IllegalArgumentException("Landlord account not found for email: " + landlordEmail));

        if (!leaseRequest.getLandlord().getId().equals(landlord.getId())) {
            throw new IllegalArgumentException("You can only update lease requests for your own properties");
        }

        if (leaseRequest.getStatus() != LeaseRequestStatus.PENDING) {
            throw new IllegalStateException("Can only update pending lease requests. Current status: " + leaseRequest.getStatus());
        }

        leaseRequest.setStatus(request.newStatus());
        leaseRequest.setLandlordRemarks(request.remarks());
        leaseRequest.setReviewedAt(LocalDateTime.now());

        LeaseRequest updated = leaseRequestRepository.save(leaseRequest);
        return toResponse(updated);
    }

    @Override
    @Transactional
    public void cancelLeaseRequest(UUID id, String applicantEmail) {
        LeaseRequest leaseRequest = leaseRequestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Lease request not found: " + id));

        Citizen applicant = citizenRepository.findByEmail(applicantEmail)
                .orElseThrow(() -> new IllegalArgumentException("Applicant account not found for email: " + applicantEmail));

        if (!leaseRequest.getApplicant().getId().equals(applicant.getId())) {
            throw new IllegalArgumentException("You can only cancel your own lease requests");
        }

        if (leaseRequest.getStatus() != LeaseRequestStatus.PENDING) {
            throw new IllegalStateException("Can only cancel pending lease requests. Current status: " + leaseRequest.getStatus());
        }

        leaseRequest.setStatus(LeaseRequestStatus.CANCELLED);
        leaseRequestRepository.save(leaseRequest);
    }

    @Override
    public List<LeaseRequestResponse> getPendingRequestsForProperty(UUID propertyId, String landlordEmail) {
        Citizen landlord = citizenRepository.findByEmail(landlordEmail)
                .orElseThrow(() -> new IllegalArgumentException("Landlord account not found for email: " + landlordEmail));

        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new IllegalArgumentException("Property not found: " + propertyId));

        if (!property.getLandlord().getId().equals(landlord.getId())) {
            throw new IllegalArgumentException("You can only view requests for your own properties");
        }

        List<LeaseRequest> requests = leaseRequestRepository.findByPropertyId(propertyId).stream()
                .filter(lr -> lr.getStatus() == LeaseRequestStatus.PENDING)
                .toList();
        
        return requests.stream().map(this::toResponse).toList();
    }

    @Override
    public List<LeaseRequestResponse> getPendingRequestsForUnit(UUID unitId, String landlordEmail) {
        Citizen landlord = citizenRepository.findByEmail(landlordEmail)
                .orElseThrow(() -> new IllegalArgumentException("Landlord account not found for email: " + landlordEmail));

        PropertyUnit unit = propertyUnitRepository.findById(unitId)
                .orElseThrow(() -> new IllegalArgumentException("Unit not found: " + unitId));

        if (!unit.getProperty().getLandlord().getId().equals(landlord.getId())) {
            throw new IllegalArgumentException("You can only view requests for your own properties");
        }

        List<LeaseRequest> requests = leaseRequestRepository.findByUnitId(unitId).stream()
                .filter(lr -> lr.getStatus() == LeaseRequestStatus.PENDING)
                .toList();
        
        return requests.stream().map(this::toResponse).toList();
    }

    private LeaseRequestResponse toResponse(LeaseRequest leaseRequest) {
        return new LeaseRequestResponse(
                leaseRequest.getId(),
                leaseRequest.getProperty().getId(),
                leaseRequest.getProperty().getPropertyCode(),
                leaseRequest.getProperty().getTitle(),
                leaseRequest.getUnit() != null ? leaseRequest.getUnit().getId() : null,
                leaseRequest.getUnit() != null ? leaseRequest.getUnit().getUnitCode() : null,
                leaseRequest.getApplicant().getId(),
                leaseRequest.getApplicant().getFirstName() + " " + leaseRequest.getApplicant().getLastName(),
                leaseRequest.getApplicant().getEmail(),
                leaseRequest.getLandlord().getId(),
                leaseRequest.getLandlord().getFirstName() + " " + leaseRequest.getLandlord().getLastName(),
                leaseRequest.getLandlord().getEmail(),
                leaseRequest.getProposedRent(),
                leaseRequest.getLeaseDurationMonths(),
                leaseRequest.getApplicantNotes(),
                leaseRequest.getLandlordRemarks(),
                leaseRequest.getStatus(),
                leaseRequest.getCreatedAt(),
                leaseRequest.getReviewedAt(),
                leaseRequest.getExpiresAt()
        );
    }
}
