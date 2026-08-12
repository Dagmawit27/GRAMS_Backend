package com.ethiorental.backend.property.repository;

import com.ethiorental.backend.IAM.entity.Citizen;
import com.ethiorental.backend.property.entity.Property;
import com.ethiorental.backend.property.enums.PropertyStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PropertyRepository extends JpaRepository<Property, UUID> {
    List<Property> findByLandlord(Citizen landlord);
    List<Property> findByStatus(PropertyStatus status);
    List<Property> findByLandlordAndStatus(Citizen landlord, PropertyStatus status);
}
