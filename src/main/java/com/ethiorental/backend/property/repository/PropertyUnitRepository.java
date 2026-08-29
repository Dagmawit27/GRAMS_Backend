package com.ethiorental.backend.property.repository;

import com.ethiorental.backend.property.entity.PropertyUnit;
import com.ethiorental.backend.property.enums.UnitStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PropertyUnitRepository extends JpaRepository<PropertyUnit, UUID> {

    List<PropertyUnit> findByPropertyId(UUID propertyId);

    Optional<PropertyUnit> findByUnitCode(String unitCode);

    List<PropertyUnit> findByPropertyIdAndStatus(UUID propertyId, UnitStatus status);

    List<PropertyUnit> findByStatus(UnitStatus status);

    @Query("SELECT u FROM PropertyUnit u WHERE u.property.id = :propertyId AND u.unitCode = :unitCode")
    Optional<PropertyUnit> findByPropertyIdAndUnitCode(@Param("propertyId") UUID propertyId, @Param("unitCode") String unitCode);

    @Query("SELECT COUNT(u) FROM PropertyUnit u WHERE u.property.id = :propertyId")
    long countByPropertyId(@Param("propertyId") UUID propertyId);

    @Query("SELECT COUNT(u) FROM PropertyUnit u WHERE u.property.id = :propertyId AND u.status = :status")
    long countByPropertyIdAndStatus(@Param("propertyId") UUID propertyId, @Param("status") UnitStatus status);
}
