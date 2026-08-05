package com.ethiorental.backend.IAM.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ethiorental.backend.IAM.entity.GovernmentEmployee;
import com.ethiorental.backend.IAM.entity.User;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface GovernmentEmployeeRepository extends JpaRepository<GovernmentEmployee, UUID> {
    Optional<GovernmentEmployee> findByUser(User user);
    Optional<GovernmentEmployee> findByUserId(UUID userId);
    Optional<GovernmentEmployee> findByEmployeeNumber(String employeeNumber);
    boolean existsByEmployeeNumber(String employeeNumber);
}
