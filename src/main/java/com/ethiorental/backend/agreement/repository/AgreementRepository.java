package com.ethiorental.backend.agreement.repository;

import com.ethiorental.backend.agreement.entity.Agreement;
import com.ethiorental.backend.agreement.enums.AgreementStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AgreementRepository extends JpaRepository<Agreement, UUID> {

    Optional<Agreement> findByAgreementNumber(String agreementNumber);

    Optional<Agreement> findByRequestCode(String requestCode);

    @Query("SELECT a FROM Agreement a WHERE TRIM(LOWER(a.landlord.email)) = TRIM(LOWER(:email)) ORDER BY a.createdAt DESC")
    List<Agreement> findByLandlordEmail(@Param("email") String email);

    @Query("SELECT a FROM Agreement a WHERE TRIM(LOWER(a.tenant.email)) = TRIM(LOWER(:email)) ORDER BY a.createdAt DESC")
    List<Agreement> findByTenantEmail(@Param("email") String email);

    @Query("SELECT a FROM Agreement a WHERE TRIM(LOWER(a.landlord.email)) = TRIM(LOWER(:email)) OR TRIM(LOWER(a.tenant.email)) = TRIM(LOWER(:email)) ORDER BY a.createdAt DESC")
    List<Agreement> findByLandlordEmailOrTenantEmail(@Param("email") String email);

    List<Agreement> findByStatus(AgreementStatus status);

    List<Agreement> findByStatusOrderByCreatedAtDesc(AgreementStatus status);

    List<Agreement> findAllByOrderByCreatedAtDesc();

    @Query("SELECT a FROM Agreement a LEFT JOIN FETCH a.unit LEFT JOIN FETCH a.tenant WHERE a.property.id = :propertyId AND a.status = :status")
    List<Agreement> findByPropertyIdAndStatus(@Param("propertyId") UUID propertyId, @Param("status") AgreementStatus status);

    @Query("SELECT a FROM Agreement a LEFT JOIN FETCH a.unit LEFT JOIN FETCH a.tenant WHERE a.property.id = :propertyId")
    List<Agreement> findByPropertyId(@Param("propertyId") UUID propertyId);
}
