package com.ethiorental.backend.IAM.repository;

import com.ethiorental.backend.IAM.entity.CitizenCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CitizenCredentialRepository extends JpaRepository<CitizenCredential, UUID> {
    Optional<CitizenCredential> findByUsername(String username);
    boolean existsByUsername(String username);
}
