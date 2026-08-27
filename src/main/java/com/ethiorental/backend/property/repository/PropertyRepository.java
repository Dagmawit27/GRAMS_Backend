package com.ethiorental.backend.property.repository;

import com.ethiorental.backend.IAM.entity.Citizen;
import com.ethiorental.backend.property.entity.Property;
import com.ethiorental.backend.property.enums.PropertyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface PropertyRepository extends JpaRepository<Property, UUID> {
    @Query("SELECT p FROM Property p LEFT JOIN FETCH p.landlord LEFT JOIN FETCH p.address WHERE p.landlord = :landlord")
    List<Property> findByLandlord(@org.springframework.data.repository.query.Param("landlord") Citizen landlord);
    
    @Query("SELECT p FROM Property p LEFT JOIN FETCH p.landlord LEFT JOIN FETCH p.address WHERE p.status = :status")
    List<Property> findByStatus(@org.springframework.data.repository.query.Param("status") PropertyStatus status);
    
    @Query("SELECT p FROM Property p LEFT JOIN FETCH p.landlord LEFT JOIN FETCH p.address WHERE p.landlord = :landlord AND p.status = :status")
    List<Property> findByLandlordAndStatus(@org.springframework.data.repository.query.Param("landlord") Citizen landlord,
                                           @org.springframework.data.repository.query.Param("status") PropertyStatus status);

    /** Properties within a specific sub-city + woreda, optionally filtered by status. */
    @Query("SELECT p FROM Property p LEFT JOIN FETCH p.landlord LEFT JOIN FETCH p.address WHERE LOWER(p.address.subCity) = LOWER(:subCity) AND p.address.woreda = :woreda AND p.status = :status")
    List<Property> findByJurisdiction(@org.springframework.data.repository.query.Param("subCity") String subCity,
                                       @org.springframework.data.repository.query.Param("woreda") String woreda,
                                       @org.springframework.data.repository.query.Param("status") PropertyStatus status);

    /** Find property by ID with landlord and address eagerly loaded to avoid lazy loading exceptions. */
    @Query("SELECT p FROM Property p LEFT JOIN FETCH p.landlord LEFT JOIN FETCH p.address WHERE p.id = :id")
    Property findWithDetailsById(@org.springframework.data.repository.query.Param("id") UUID id);

    // ── Reporting queries ─────────────────────────────────────────────────────
    long countByStatus(PropertyStatus status);

    /** Returns [status, count] pairs for all statuses that have at least one property. */
    @Query("SELECT p.status, COUNT(p) FROM Property p GROUP BY p.status")
    List<Object[]> countGroupedByStatus();

    /** Returns [propertyType, count] pairs. */
    @Query("SELECT p.propertyType, COUNT(p) FROM Property p GROUP BY p.propertyType")
    List<Object[]> countGroupedByType();
}

