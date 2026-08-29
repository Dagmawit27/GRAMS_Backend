package com.ethiorental.backend.property.repository;

import com.ethiorental.backend.property.entity.OwnershipDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface OwnershipDocumentRepository extends JpaRepository<OwnershipDocument, UUID> {
}
