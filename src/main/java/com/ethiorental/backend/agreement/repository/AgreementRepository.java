package com.ethiorental.backend.agreement.repository;

import com.ethiorental.backend.agreement.entity.Agreement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AgreementRepository extends JpaRepository<Agreement, Long> {
    Optional<Agreement> findByAgreementCode(String agreementCode);
    Optional<Agreement> findByRequestCode(String requestCode);
    boolean existsByAgreementCode(String agreementCode);
    boolean existsByRequestCode(String requestCode);
}
