package com.ethiorental.backend.lease.repository;

import com.ethiorental.backend.lease.entity.LeaseRequest;
import com.ethiorental.backend.lease.enums.LeaseRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LeaseRequestRepository extends JpaRepository<LeaseRequest, UUID> {

    List<LeaseRequest> findByApplicantId(UUID applicantId);

    List<LeaseRequest> findByLandlordId(UUID landlordId);

    List<LeaseRequest> findByPropertyId(UUID propertyId);

    List<LeaseRequest> findByUnitId(UUID unitId);

    List<LeaseRequest> findByStatus(LeaseRequestStatus status);

    Optional<LeaseRequest> findByPropertyIdAndApplicantId(UUID propertyId, UUID applicantId);

    Optional<LeaseRequest> findByUnitIdAndApplicantId(UUID unitId, UUID applicantId);

    Optional<LeaseRequest> findByRequestCode(String requestCode);

    @Query("SELECT lr FROM LeaseRequest lr WHERE lr.applicant.id = :applicantId AND lr.status = :status")
    List<LeaseRequest> findByApplicantIdAndStatus(@Param("applicantId") UUID applicantId, @Param("status") LeaseRequestStatus status);

    @Query("SELECT lr FROM LeaseRequest lr WHERE lr.landlord.id = :landlordId AND lr.status = :status")
    List<LeaseRequest> findByLandlordIdAndStatus(@Param("landlordId") UUID landlordId, @Param("status") LeaseRequestStatus status);

    @Query("SELECT COUNT(lr) FROM LeaseRequest lr WHERE lr.property.id = :propertyId AND lr.status = 'PENDING'")
    long countPendingRequestsForProperty(@Param("propertyId") UUID propertyId);

    @Query("SELECT COUNT(lr) FROM LeaseRequest lr WHERE lr.unit.id = :unitId AND lr.status = 'PENDING'")
    long countPendingRequestsForUnit(@Param("unitId") UUID unitId);
}
