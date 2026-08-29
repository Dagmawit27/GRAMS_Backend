package com.ethiorental.backend.property.repository;

import com.ethiorental.backend.property.entity.PropertyVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PropertyVerificationRepository extends JpaRepository<PropertyVerification, UUID> {
}
