package com.ethiorental.backend.IAM.repository;

import com.ethiorental.backend.IAM.entity.Citizen;
import com.ethiorental.backend.IAM.entity.CitizenRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CitizenRoleRepository extends JpaRepository<CitizenRole, UUID> {
    List<CitizenRole> findByCitizen(Citizen citizen);
}
