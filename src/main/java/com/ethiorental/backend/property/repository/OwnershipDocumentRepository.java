package com.ethiorental.backend.property.repository;

import com.ethiorental.backend.property.entity.OwnershipDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OwnershipDocumentRepository extends JpaRepository<OwnershipDocument, UUID> {
}
