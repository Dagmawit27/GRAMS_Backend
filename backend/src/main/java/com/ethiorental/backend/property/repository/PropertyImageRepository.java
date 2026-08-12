package com.ethiorental.backend.property.repository;

import com.ethiorental.backend.property.entity.PropertyImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PropertyImageRepository extends JpaRepository<PropertyImage, UUID> {
}
