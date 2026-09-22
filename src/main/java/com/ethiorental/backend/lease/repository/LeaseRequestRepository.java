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

    @Query("SELECT lr FROM LeaseRequest lr LEFT JOIN FETCH lr.landlord LEFT JOIN FETCH lr.property LEFT JOIN FETCH lr.unit WHERE lr.applicant.id = :applicantId")
    List<LeaseRequest> findByApplicantIdWithDetails(@Param("applicantId") UUID applicantId);

    List<LeaseRequest> findByLandlordId(UUID landlordId);

    @Query("SELECT lr FROM LeaseRequest lr LEFT JOIN FETCH lr.applicant LEFT JOIN FETCH lr.landlord LEFT JOIN FETCH lr.property LEFT JOIN FETCH lr.unit WHERE lr.landlord.id = :landlordId")
    List<LeaseRequest> findByLandlordIdWithDetails(@Param("landlordId") UUID landlordId);

    List<LeaseRequest> findByPropertyId(UUID propertyId);

    List<LeaseRequest> findByUnitId(UUID unitId);

    List<LeaseRequest> findByStatus(LeaseRequestStatus status);

    Optional<LeaseRequest> findByPropertyIdAndApplicantId(UUID propertyId, UUID applicantId);

    Optional<LeaseRequest> findByUnitIdAndApplicantId(UUID unitId, UUID applicantId);

    Optional<LeaseRequest> findByRequestCode(String requestCode);

    @Query("SELECT lr FROM LeaseRequest lr LEFT JOIN FETCH lr.applicant LEFT JOIN FETCH lr.landlord LEFT JOIN FETCH lr.property LEFT JOIN FETCH lr.unit WHERE lr.requestCode = :requestCode")
    Optional<LeaseRequest> findByRequestCodeWithDetails(@Param("requestCode") String requestCode);

    @Query("SELECT lr FROM LeaseRequest lr WHERE lr.applicant.id = :applicantId AND lr.status = :status")
    List<LeaseRequest> findByApplicantIdAndStatus(@Param("applicantId") UUID applicantId, @Param("status") LeaseRequestStatus status);

    @Query("SELECT lr FROM LeaseRequest lr WHERE lr.landlord.id = :landlordId AND lr.status = :status")
    List<LeaseRequest> findByLandlordIdAndStatus(@Param("landlordId") UUID landlordId, @Param("status") LeaseRequestStatus status);

    @Query("SELECT COUNT(lr) FROM LeaseRequest lr WHERE lr.property.id = :propertyId AND lr.status = 'PENDING'")
    long countPendingRequestsForProperty(@Param("propertyId") UUID propertyId);

    @Query("SELECT COUNT(lr) FROM LeaseRequest lr WHERE lr.unit.id = :unitId AND lr.status = 'PENDING'")
    long countPendingRequestsForUnit(@Param("unitId") UUID unitId);

    @Query("SELECT lr FROM LeaseRequest lr " +
           "LEFT JOIN FETCH lr.applicant LEFT JOIN FETCH lr.landlord " +
           "LEFT JOIN FETCH lr.property p LEFT JOIN FETCH p.address a LEFT JOIN FETCH lr.unit " +
           "WHERE lr.status = :status AND LOWER(a.subCity) = LOWER(:subCity) AND a.woreda = :woreda")
    List<LeaseRequest> findByStatusAndJurisdiction(
            @Param("status") LeaseRequestStatus status,
            @Param("subCity") String subCity,
            @Param("woreda") String woreda);
}
