package com.ethiorental.backend.payment.repository;

import com.ethiorental.backend.payment.entity.Payment;
import com.ethiorental.backend.payment.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByTxRef(String txRef);

    List<Payment> findByAgreementId(UUID agreementId);

    Optional<Payment> findByRequestCode(String requestCode);

    List<Payment> findByRequestCodeOrderByCreatedAtDesc(String requestCode);

    List<Payment> findByAgreementNumberOrderByCreatedAtDesc(String agreementNumber);

    @Query("SELECT p FROM Payment p WHERE TRIM(LOWER(p.tenantEmail)) = TRIM(LOWER(:email)) OR (p.tenant IS NOT NULL AND TRIM(LOWER(p.tenant.email)) = TRIM(LOWER(:email))) ORDER BY p.createdAt DESC")
    List<Payment> findByTenantEmail(@Param("email") String email);

    @Query("SELECT p FROM Payment p WHERE TRIM(LOWER(p.landlordEmail)) = TRIM(LOWER(:email)) OR (p.landlord IS NOT NULL AND TRIM(LOWER(p.landlord.email)) = TRIM(LOWER(:email))) ORDER BY p.createdAt DESC")
    List<Payment> findByLandlordEmail(@Param("email") String email);

    List<Payment> findByTenantId(UUID tenantId);

    List<Payment> findByLandlordId(UUID landlordId);

    List<Payment> findByStatus(PaymentStatus status);

    @Query("SELECT COUNT(p) > 0 FROM Payment p WHERE p.agreement.id = :agreementId " +
           "AND p.status = com.ethiorental.backend.payment.enums.PaymentStatus.COMPLETED " +
           "AND p.paymentDate >= :startDate AND p.paymentDate < :endDate")
    boolean existsCompletedPaymentForPeriod(
        @Param("agreementId") UUID agreementId,
        @Param("startDate") java.time.LocalDateTime startDate,
        @Param("endDate") java.time.LocalDateTime endDate);
}
