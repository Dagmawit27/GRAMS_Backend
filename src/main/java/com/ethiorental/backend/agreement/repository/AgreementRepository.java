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

    @Query("SELECT a FROM Agreement a WHERE a.landlord.email = :email ORDER BY a.createdAt DESC")
    List<Agreement> findByLandlordEmail(@Param("email") String email);

    @Query("SELECT a FROM Agreement a WHERE a.tenant.email = :email ORDER BY a.createdAt DESC")
    List<Agreement> findByTenantEmail(@Param("email") String email);

    @Query("SELECT a FROM Agreement a WHERE a.landlord.email = :email OR a.tenant.email = :email ORDER BY a.createdAt DESC")
    List<Agreement> findByLandlordEmailOrTenantEmail(@Param("email") String email);

    List<Agreement> findByStatus(AgreementStatus status);

    @Query("SELECT DISTINCT a FROM Agreement a " +
           "LEFT JOIN FETCH a.tenant " +
           "LEFT JOIN FETCH a.landlord " +
           "LEFT JOIN FETCH a.property " +
           "WHERE a.status = :status")
    List<Agreement> findByStatusWithDetails(@Param("status") AgreementStatus status);

    List<Agreement> findByStatusOrderByCreatedAtDesc(AgreementStatus status);

    List<Agreement> findAllByOrderByCreatedAtDesc();
}
