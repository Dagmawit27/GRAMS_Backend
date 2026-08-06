package com.ethiorental.backend.IAM.service;

import com.ethiorental.backend.IAM.entity.*;
import com.ethiorental.backend.IAM.enums.EmployeeStatus;
import com.ethiorental.backend.IAM.enums.Gender;
import com.ethiorental.backend.IAM.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RoleInitializerService implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final GovernmentEmployeeRepository employeeRepository;
    private final EmployeeCredentialRepository employeeCredentialRepository;
    private final EmployeeRoleRepository employeeRoleRepository;
    private final OfficeRepository officeRepository;
    private final PasswordEncoder passwordEncoder;

    private static final List<String[]> SYSTEM_ROLES = List.of(
            new String[]{"CITIZEN",                "CITIZEN"},
            new String[]{"LANDLORD",               "CITIZEN"},
            new String[]{"TENANT",                 "CITIZEN"},
            new String[]{"WOREDA_OFFICER",         "EMPLOYEE"},
            new String[]{"WOREDA_SUPERVISOR",      "EMPLOYEE"},
            new String[]{"SUB_CITY_ADMINISTRATOR", "EMPLOYEE"},
            new String[]{"CITY_ADMINISTRATOR",     "EMPLOYEE"},
            new String[]{"TAX_OFFICER",            "EMPLOYEE"},
            new String[]{"AUDITOR",                "EMPLOYEE"},
            new String[]{"SYSTEM_ADMINISTRATOR",   "EMPLOYEE"}
    );

    @Override
    @Transactional
    public void run(String... args) {
        // 1. Seed roles
        for (String[] role : SYSTEM_ROLES) {
            if (!roleRepository.existsByRoleName(role[0])) {
                roleRepository.save(Role.builder()
                        .roleName(role[0])
                        .roleType(role[1])
                        .build());
                log.info("Seeded role: {}", role[0]);
            }
        }

        // 2. Bootstrap default admin office if needed
        Office adminOffice = officeRepository.findAll().stream()
                .filter(o -> "HEAD_OFFICE".equals(o.getOfficeType()))
                .findFirst()
                .orElseGet(() -> officeRepository.save(Office.builder()
                        .officeName("GRAMS Headquarters")
                        .officeType("HEAD_OFFICE")
                        .build()));

        // 3. Bootstrap initial System Administrator if none exists
        String adminEmail = "admin@grams.gov.et";
        if (!employeeCredentialRepository.existsByUsername(adminEmail)) {
            GovernmentEmployee admin = GovernmentEmployee.builder()
                    .employeeNumber("EMP-0001")
                    .office(adminOffice)
                    .firstName("System")
                    .middleName("Admin")
                    .lastName("GRAMS")
                    .gender(Gender.MALE)
                    .phone("+251900000000")
                    .email(adminEmail)
                    .position("System Administrator")
                    .status(EmployeeStatus.ACTIVE)
                    .build();
            employeeRepository.save(admin);

            EmployeeCredential cred = EmployeeCredential.builder()
                    .employee(admin)
                    .username(adminEmail)
                    .passwordHash(passwordEncoder.encode("AdminPassword123!"))
                    .build();
            employeeCredentialRepository.save(cred);

            Role adminRole = roleRepository.findByRoleName("SYSTEM_ADMINISTRATOR")
                    .orElseThrow();
            employeeRoleRepository.save(EmployeeRole.builder()
                    .employee(admin).role(adminRole).build());

            log.info("Bootstrapped default System Administrator: {}", adminEmail);
        }
    }
}
