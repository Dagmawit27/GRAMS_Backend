package com.ethiorental.backend.IAM.repository;

import com.ethiorental.backend.IAM.entity.Citizen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CitizenRepository extends JpaRepository<Citizen, UUID> {
    Optional<Citizen> findByEmail(String email);
    Optional<Citizen> findByPhone(String phone);
    Optional<Citizen> findByFaydaId(String faydaId);
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);
    boolean existsByFaydaId(String faydaId);
}
