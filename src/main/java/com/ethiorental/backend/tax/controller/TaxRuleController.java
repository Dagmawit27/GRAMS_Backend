package com.ethiorental.backend.tax.controller;

import com.ethiorental.backend.tax.entity.TaxRule;
import com.ethiorental.backend.tax.repository.TaxRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tax-rules")
@RequiredArgsConstructor
@Slf4j
public class TaxRuleController {

    private final TaxRuleRepository taxRuleRepository;

    /**
     * Get active tax rule (public endpoint for tax calculation / reference)
     */
    @GetMapping("/active")
    public ResponseEntity<TaxRule> getActiveTaxRule() {
        return taxRuleRepository.findFirstByIsActiveTrueOrderByEffectiveFromDesc()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get all tax rules (TAX_OFFICER only)
     */
    @GetMapping
    @PreAuthorize("hasRole('TAX_OFFICER')")
    public ResponseEntity<List<TaxRule>> getAllTaxRules() {
        return ResponseEntity.ok(taxRuleRepository.findByIsActiveTrue());
    }

    /**
     * Create new tax rule (TAX_OFFICER only)
     */
    @PostMapping
    @PreAuthorize("hasRole('TAX_OFFICER')")
    public ResponseEntity<TaxRule> createTaxRule(@RequestBody TaxRule taxRule, @RequestHeader(value = "X-User-Email", required = false) String userEmail) {
        String actor = userEmail != null ? userEmail : "SYSTEM_OFFICER";
        taxRuleRepository.findByIsActiveTrue().forEach(rule -> {
            rule.setIsActive(false);
            rule.setEffectiveTo(LocalDate.now().minusDays(1));
            rule.setUpdatedBy(actor);
            taxRuleRepository.save(rule);
        });

        taxRule.setIsActive(true);
        taxRule.setCreatedBy(actor);
        taxRule.setUpdatedBy(actor);
        TaxRule saved = taxRuleRepository.save(taxRule);
        log.info("New tax rule created by {}: proclamation={}, deduction={}%",
                actor, taxRule.getProclamationNumber(), taxRule.getDeductionPercentage());
        return ResponseEntity.ok(saved);
    }

    /**
     * Update existing tax rule (TAX_OFFICER only)
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('TAX_OFFICER')")
    public ResponseEntity<TaxRule> updateTaxRule(
            @PathVariable UUID id,
            @RequestBody TaxRule taxRule,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail) {
        String actor = userEmail != null ? userEmail : "SYSTEM_OFFICER";
        return taxRuleRepository.findById(id)
                .map(existing -> {
                    existing.setProclamationNumber(taxRule.getProclamationNumber());
                    existing.setProclamationYear(taxRule.getProclamationYear());
                    existing.setDeductionPercentage(taxRule.getDeductionPercentage());
                    existing.setTaxFreeThreshold(taxRule.getTaxFreeThreshold());
                    existing.setBracket1Rate(taxRule.getBracket1Rate());
                    existing.setBracket1Min(taxRule.getBracket1Min());
                    existing.setBracket1Max(taxRule.getBracket1Max());
                    existing.setBracket2Rate(taxRule.getBracket2Rate());
                    existing.setBracket2Min(taxRule.getBracket2Min());
                    existing.setBracket2Max(taxRule.getBracket2Max());
                    existing.setBracket3Rate(taxRule.getBracket3Rate());
                    existing.setBracket3Min(taxRule.getBracket3Min());
                    existing.setBracket3Max(taxRule.getBracket3Max());
                    existing.setBracket4Rate(taxRule.getBracket4Rate());
                    existing.setBracket4Min(taxRule.getBracket4Min());
                    existing.setBracket4Max(taxRule.getBracket4Max());
                    existing.setBracket5Rate(taxRule.getBracket5Rate());
                    existing.setBracket5Min(taxRule.getBracket5Min());
                    existing.setCorporateTaxRate(taxRule.getCorporateTaxRate());
                    existing.setEffectiveFrom(taxRule.getEffectiveFrom());
                    existing.setEffectiveTo(taxRule.getEffectiveTo());
                    existing.setDescription(taxRule.getDescription());
                    existing.setUpdatedBy(actor);
                    TaxRule updated = taxRuleRepository.save(existing);
                    log.info("Tax rule {} updated by {}", id, actor);
                    return ResponseEntity.ok(updated);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Delete tax rule (TAX_OFFICER only)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('TAX_OFFICER')")
    public ResponseEntity<Void> deleteTaxRule(@PathVariable UUID id, @RequestHeader(value = "X-User-Email", required = false) String userEmail) {
        if (taxRuleRepository.existsById(id)) {
            taxRuleRepository.deleteById(id);
            log.info("Tax rule {} deleted by {}", id, userEmail);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
