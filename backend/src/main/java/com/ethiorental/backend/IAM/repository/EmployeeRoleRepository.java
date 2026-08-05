package com.ethiorental.backend.IAM.repository;

import com.ethiorental.backend.IAM.entity.EmployeeRole;
import com.ethiorental.backend.IAM.entity.GovernmentEmployee;
import com.ethiorental.backend.IAM.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EmployeeRoleRepository extends JpaRepository<EmployeeRole, UUID> {
    List<EmployeeRole> findByEmployee(GovernmentEmployee employee);
    List<EmployeeRole> findByEmployeeId(UUID employeeId);
    boolean existsByEmployeeAndRole(GovernmentEmployee employee, Role role);
}
