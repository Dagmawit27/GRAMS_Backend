package com.ethiorental.backend.IAM.repository;

import com.ethiorental.backend.IAM.entity.Role;
import com.ethiorental.backend.IAM.entity.User;
import com.ethiorental.backend.IAM.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, UUID> {
    List<UserRole> findByUser(User user);
    List<UserRole> findByUserId(UUID userId);
    boolean existsByUserAndRole(User user, Role role);
}
