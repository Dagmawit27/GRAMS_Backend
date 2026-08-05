package com.ethiorental.backend.IAM.service;

import com.ethiorental.backend.IAM.entity.Role;
import com.ethiorental.backend.IAM.entity.User;
import com.ethiorental.backend.IAM.entity.UserRole;
import com.ethiorental.backend.IAM.enums.Gender;
import com.ethiorental.backend.IAM.enums.Status;
import com.ethiorental.backend.IAM.repository.RoleRepository;
import com.ethiorental.backend.IAM.repository.UserRepository;
import com.ethiorental.backend.IAM.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RoleInitializerService implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;

    public static final List<String> SYSTEM_ROLES = List.of(
            "LANDLORD",
            "TENANT",
            "WOREDA_OFFICER",
            "WOREDA_SUPERVISOR",
            "SUB_CITY_ADMINISTRATOR",
            "CITY_ADMINISTRATOR",
            "TAX_OFFICER",
            "SYSTEM_ADMINISTRATOR",
            "AUDITOR"
    );

    @Override
    public void run(String... args) {
        // 1. Seed System Roles
        for (String roleName : SYSTEM_ROLES) {
            if (!roleRepository.existsByRoleName(roleName)) {
                Role role = new Role();
                role.setRoleName(roleName);
                roleRepository.save(role);
            }
        }

        // 2. Bootstrap Initial System Administrator if no admin exists
        String adminEmail = "admin@grams.gov.et";
        if (!userRepository.existsByEmail(adminEmail)) {
            User admin = new User();
            admin.setFaydaId(100000000000L);
            admin.setFirstName("System");
            admin.setMiddleName("Admin");
            admin.setLastName("Super");
            admin.setGender(Gender.MALE);
            admin.setDateOfBirth(LocalDate.of(1990, 1, 1));
            admin.setEmail(adminEmail);
            admin.setPhoneNumber("+251900000000");
            admin.setPassword(passwordEncoder.encode("AdminPassword123!"));
            admin.setStatus(Status.ACTIVE);
            admin = userRepository.save(admin);

            Role adminRole = roleRepository.findByRoleName("SYSTEM_ADMINISTRATOR").orElseThrow();
            UserRole userRole = new UserRole();
            userRole.setUser(admin);
            userRole.setRole(adminRole);
            userRoleRepository.save(userRole);

            log.info("Initialized default System Administrator account: {}", adminEmail);
        }
    }
}
