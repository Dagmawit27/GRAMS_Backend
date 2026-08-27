package com.ethiorental.backend.IAM.service;

import com.ethiorental.backend.IAM.entity.*;
import com.ethiorental.backend.IAM.enums.EmployeeStatus;
import com.ethiorental.backend.IAM.enums.Gender;
import com.ethiorental.backend.IAM.repository.*;
import com.ethiorental.backend.location.entity.SubCityWoreda;
import com.ethiorental.backend.location.repository.SubCityWoredaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class RoleInitializerService implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final GovernmentEmployeeRepository employeeRepository;
    private final EmployeeCredentialRepository employeeCredentialRepository;
    private final EmployeeRoleRepository employeeRoleRepository;
    private final OfficeRepository officeRepository;
    private final SubCityWoredaRepository subCityWoredaRepository;
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

    /**
     * Official Addis Ababa sub-cities mapped to their woreda counts.
     * Source: Addis Ababa City Administration.
     */
    private static final Map<String, Integer> AA_SUB_CITIES = Map.ofEntries(
            Map.entry("Akaky Kaliti",      13),
            Map.entry("Addis Ketema",      10),
            Map.entry("Arada",              8),
            Map.entry("Bole",              14),
            Map.entry("Gullele",           10),
            Map.entry("Kirkos",            10),
            Map.entry("Kolfe Keranio",     15),
            Map.entry("Lemi Kura",         12),
            Map.entry("Lideta",            10),
            Map.entry("Nifas Silk-Lafto",  14),
            Map.entry("Yeka",              13)
    );

    @Override
    @Transactional
    public void run(String... args) {

        // ── 1. Seed system roles ─────────────────────────────────────────────
        for (String[] role : SYSTEM_ROLES) {
            if (!roleRepository.existsByRoleName(role[0])) {
                roleRepository.save(Role.builder()
                        .roleName(role[0])
                        .roleType(role[1])
                        .build());
                log.info("Seeded role: {}", role[0]);
            }
        }

        // ── 2. Seed Addis Ababa sub-city / woreda reference data ─────────────
        for (Map.Entry<String, Integer> entry : AA_SUB_CITIES.entrySet()) {
            String subCity = entry.getKey();
            int woredaCount = entry.getValue();
            for (int i = 1; i <= woredaCount; i++) {
                String woreda = String.format("%02d", i);
                if (!subCityWoredaRepository.existsBySubCityIgnoreCaseAndWoreda(subCity, woreda)) {
                    subCityWoredaRepository.save(SubCityWoreda.builder()
                            .subCity(subCity)
                            .woreda(woreda)
                            .build());
                }
            }
        }
        log.info("Sub-city/woreda reference data seeded.");

        // ── 3. Bootstrap default admin office ────────────────────────────────
        Office adminOffice = officeRepository.findAll().stream()
                .filter(o -> "HEAD_OFFICE".equals(o.getOfficeType()))
                .findFirst()
                .orElseGet(() -> officeRepository.save(Office.builder()
                        .officeName("GRAMS Headquarters")
                        .officeType("HEAD_OFFICE")
                        .subCity("Addis Ababa")
                        .woreda("00")
                        .build()));

        // ── 4. Bootstrap System Administrator ────────────────────────────────
        String adminEmail = "admin@grams.gov.et";
        if (!employeeCredentialRepository.existsByEmail(adminEmail)) {
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

            employeeCredentialRepository.save(EmployeeCredential.builder()
                    .employee(admin)
                    .email(adminEmail)
                    .passwordHash(passwordEncoder.encode("AdminPassword123!"))
                    .build());

            roleRepository.findByRoleName("SYSTEM_ADMINISTRATOR").ifPresent(role ->
                    employeeRoleRepository.save(EmployeeRole.builder()
                            .employee(admin).role(role).build()));

            log.info("Bootstrapped System Administrator: {}", adminEmail);
        }

        // ── 5. Bootstrap sample Woreda Office (Bole 03) ──────────────────────
        Office woredaOffice = officeRepository
                .findBySubCityIgnoreCaseAndWoreda("Bole", "03")
                .orElseGet(() -> officeRepository.save(Office.builder()
                        .officeName("Bole Woreda 03 Housing Desk")
                        .officeType("WOREDA_OFFICE")
                        .subCity("Bole")
                        .woreda("03")
                        .parentOffice(adminOffice)
                        .build()));

        // ── 6. Bootstrap sample Woreda Officer ───────────────────────────────
        String officerEmail = "officer03@gmail.com";
        if (!employeeCredentialRepository.existsByEmail(officerEmail)) {
            GovernmentEmployee officer = GovernmentEmployee.builder()
                    .employeeNumber("EMP-0005")
                    .office(woredaOffice)
                    .firstName("Abebe")
                    .middleName("Tadesse")
                    .lastName("Bekele")
                    .gender(Gender.MALE)
                    .phone("+251911110001")
                    .email(officerEmail)
                    .position("Woreda Property Officer")
                    .status(EmployeeStatus.ACTIVE)
                    .build();
            employeeRepository.save(officer);

            employeeCredentialRepository.save(EmployeeCredential.builder()
                    .employee(officer)
                    .email(officerEmail)
                    .passwordHash(passwordEncoder.encode("12345678"))
                    .build());

            roleRepository.findByRoleName("WOREDA_OFFICER").ifPresent(role ->
                    employeeRoleRepository.save(EmployeeRole.builder()
                            .employee(officer).role(role).build()));

            log.info("Bootstrapped sample Woreda Officer: {}", officerEmail);
        }

        // ── 7. Bootstrap sample Woreda Supervisor ────────────────────────────
        String supervisorEmail = "supervisor03@gmail.com";
        if (!employeeCredentialRepository.existsByEmail(supervisorEmail)) {
            GovernmentEmployee supervisor = GovernmentEmployee.builder()
                    .employeeNumber("EMP-0006")
                    .office(woredaOffice)
                    .firstName("Tigist")
                    .middleName("Haile")
                    .lastName("Mekonen")
                    .gender(Gender.FEMALE)
                    .phone("+251911100003")
                    .email(supervisorEmail)
                    .position("Woreda Property Supervisor")
                    .status(EmployeeStatus.ACTIVE)
                    .build();
            employeeRepository.save(supervisor);

            employeeCredentialRepository.save(EmployeeCredential.builder()
                    .employee(supervisor)
                    .email(supervisorEmail)
                    .passwordHash(passwordEncoder.encode("12345678"))
                    .build());

            roleRepository.findByRoleName("WOREDA_SUPERVISOR").ifPresent(role ->
                    employeeRoleRepository.save(EmployeeRole.builder()
                            .employee(supervisor).role(role).build()));
            log.info("Bootstrapped sample Woreda Supervisor: {}", supervisorEmail);
        }
    }
}
