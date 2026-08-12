package com.ethiorental.backend.property.repository;

import com.ethiorental.backend.property.entity.PropertyVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PropertyVerificationRepository extends JpaRepository<PropertyVerification, UUID> {
}
