package com.ethiorental.backend.IAM.repository;

import com.ethiorental.backend.IAM.entity.GovernmentEmployee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface GovernmentEmployeeRepository extends JpaRepository<GovernmentEmployee, UUID> {
    Optional<GovernmentEmployee> findByEmployeeNumber(String employeeNumber);
    boolean existsByEmployeeNumber(String employeeNumber);
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);
}
