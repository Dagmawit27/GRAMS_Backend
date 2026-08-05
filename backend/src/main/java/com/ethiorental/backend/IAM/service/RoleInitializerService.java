package com.ethiorental.backend.IAM.service;

import com.ethiorental.backend.IAM.entity.Role;
import com.ethiorental.backend.IAM.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class RoleInitializerService implements CommandLineRunner {

    private final RoleRepository roleRepository;

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
        for (String roleName : SYSTEM_ROLES) {
            if (!roleRepository.existsByRoleName(roleName)) {
                Role role = new Role();
                role.setRoleName(roleName);
                roleRepository.save(role);
            }
        }
    }
}
