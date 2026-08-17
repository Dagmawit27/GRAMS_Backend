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

        // 4. Bootstrap sample Woreda Office
        Office woredaOffice = officeRepository.findAll().stream()
                .filter(o -> "WOREDA_OFFICE".equals(o.getOfficeType()))
                .findFirst()
                .orElseGet(() -> officeRepository.save(Office.builder()
                        .officeName("Bole Woreda 03 Office")
                        .officeType("WOREDA_OFFICE")
                        .parentOffice(adminOffice)
                        .build()));

        // 5. Bootstrap sample Woreda Officer
        String officerEmail = "officer@grams.gov.et";
        if (!employeeCredentialRepository.existsByUsername(officerEmail)) {
            GovernmentEmployee officer = GovernmentEmployee.builder()
                    .employeeNumber("EMP-0002")
                    .office(woredaOffice)
                    .firstName("Abebe")
                    .middleName("Tadesse")
                    .lastName("Bekele")
                    .gender(Gender.MALE)
                    .phone("+251911100001")
                    .email(officerEmail)
                    .position("Woreda Property Officer")
                    .status(EmployeeStatus.ACTIVE)
                    .build();
            employeeRepository.save(officer);

            employeeCredentialRepository.save(EmployeeCredential.builder()
                    .employee(officer)
                    .username(officerEmail)
                    .passwordHash(passwordEncoder.encode("Officer@1234"))
                    .build());

            Role officerRole = roleRepository.findByRoleName("WOREDA_OFFICER").orElseThrow();
            employeeRoleRepository.save(EmployeeRole.builder()
                    .employee(officer).role(officerRole).build());

            log.info("Bootstrapped sample Woreda Officer: {}", officerEmail);
        }

        // 6. Bootstrap sample Woreda Supervisor
        String supervisorEmail = "supervisor@grams.gov.et";
        if (!employeeCredentialRepository.existsByUsername(supervisorEmail)) {
            GovernmentEmployee supervisor = GovernmentEmployee.builder()
                    .employeeNumber("EMP-0003")
                    .office(woredaOffice)
                    .firstName("Tigist")
                    .middleName("Haile")
                    .lastName("Mekonen")
                    .gender(Gender.FEMALE)
                    .phone("+251911100002")
                    .email(supervisorEmail)
                    .position("Woreda Property Supervisor")
                    .status(EmployeeStatus.ACTIVE)
                    .build();
            employeeRepository.save(supervisor);

            employeeCredentialRepository.save(EmployeeCredential.builder()
                    .employee(supervisor)
                    .username(supervisorEmail)
                    .passwordHash(passwordEncoder.encode("Supervisor@1234"))
                    .build());

            Role supervisorRole = roleRepository.findByRoleName("WOREDA_SUPERVISOR").orElseThrow();
            employeeRoleRepository.save(EmployeeRole.builder()
                    .employee(supervisor).role(supervisorRole).build());

            log.info("Bootstrapped sample Woreda Supervisor: {}", supervisorEmail);
        }
    }
}
