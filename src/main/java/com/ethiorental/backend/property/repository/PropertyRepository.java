package com.ethiorental.backend.property.repository;

import com.ethiorental.backend.IAM.entity.Citizen;
import com.ethiorental.backend.property.entity.Property;
import com.ethiorental.backend.property.enums.PropertyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface PropertyRepository extends JpaRepository<Property, UUID> {
    List<Property> findByLandlord(Citizen landlord);
    List<Property> findByStatus(PropertyStatus status);
    List<Property> findByLandlordAndStatus(Citizen landlord, PropertyStatus status);

    // ── Reporting queries ─────────────────────────────────────────────────────
    long countByStatus(PropertyStatus status);

    /** Returns [status, count] pairs for all statuses that have at least one property. */
    @Query("SELECT p.status, COUNT(p) FROM Property p GROUP BY p.status")
    List<Object[]> countGroupedByStatus();

    /** Returns [propertyType, count] pairs. */
    @Query("SELECT p.propertyType, COUNT(p) FROM Property p GROUP BY p.propertyType")
    List<Object[]> countGroupedByType();
}

