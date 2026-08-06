package com.ethiorental.backend.IAM.repository;

import com.ethiorental.backend.IAM.entity.Office;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface OfficeRepository extends JpaRepository<Office, UUID> {
}
