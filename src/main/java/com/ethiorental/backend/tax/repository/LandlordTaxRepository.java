package com.ethiorental.backend.tax.repository;

import com.ethiorental.backend.tax.entity.LandlordTax;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LandlordTaxRepository extends JpaRepository<LandlordTax, UUID> {

    Optional<LandlordTax> findByLandlordEmailAndFiscalYear(String landlordEmail, String fiscalYear);

    Optional<LandlordTax> findFirstByLandlordEmailOrderByCreatedAtDesc(String landlordEmail);

    List<LandlordTax> findByFiscalYear(String fiscalYear);
}
