package com.ethiorental.backend.tax.repository;

import com.ethiorental.backend.tax.entity.TaxRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TaxRuleRepository extends JpaRepository<TaxRule, UUID> {

    Optional<TaxRule> findFirstByIsActiveTrueOrderByEffectiveFromDesc();

    List<TaxRule> findByIsActiveTrue();
}
