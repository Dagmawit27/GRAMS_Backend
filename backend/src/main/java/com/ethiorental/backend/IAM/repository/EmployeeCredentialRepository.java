package com.ethiorental.backend.IAM.repository;

import com.ethiorental.backend.IAM.entity.EmployeeCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmployeeCredentialRepository extends JpaRepository<EmployeeCredential, UUID> {
    Optional<EmployeeCredential> findByUsername(String username);
    boolean existsByUsername(String username);
}
