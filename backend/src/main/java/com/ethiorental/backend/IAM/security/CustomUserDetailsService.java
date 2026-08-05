package com.ethiorental.backend.IAM.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ethiorental.backend.IAM.entity.GovernmentEmployee;
import com.ethiorental.backend.IAM.entity.User;
import com.ethiorental.backend.IAM.repository.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final GovernmentEmployeeRepository governmentEmployeeRepository;
    private final EmployeeRoleRepository employeeRoleRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        User user = findUserByIdentifier(identifier)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with identifier: " + identifier));

        List<String> roles = extractRolesForUser(user);
        return new CustomUserDetails(user, roles);
    }

    public Optional<User> findUserByIdentifier(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return Optional.empty();
        }
        // Try search by email
        Optional<User> byEmail = userRepository.findByEmail(identifier);
        if (byEmail.isPresent()) return byEmail;

        // Try search by phone
        Optional<User> byPhone = userRepository.findByPhoneNumber(identifier);
        if (byPhone.isPresent()) return byPhone;

        // Try search by Fayda ID if numeric
        try {
            Long faydaId = Long.parseLong(identifier);
            return userRepository.findByFaydaId(faydaId);
        } catch (NumberFormatException ignored) {
        }

        return Optional.empty();
    }

    public List<String> extractRolesForUser(User user) {
        List<String> roleNames = new ArrayList<>();

        // 1. Roles from user_roles table
        userRoleRepository.findByUser(user).forEach(ur -> {
            if (ur.getRole() != null && ur.getRole().getRoleName() != null) {
                roleNames.add(ur.getRole().getRoleName());
            }
        });

        // 2. Roles from employee_roles table if user is a government employee
        Optional<GovernmentEmployee> empOpt = governmentEmployeeRepository.findByUser(user);
        if (empOpt.isPresent()) {
            employeeRoleRepository.findByEmployee(empOpt.get()).forEach(er -> {
                if (er.getRole() != null && er.getRole().getRoleName() != null) {
                    if (!roleNames.contains(er.getRole().getRoleName())) {
                        roleNames.add(er.getRole().getRoleName());
                    }
                }
            });
        }

        return roleNames;
    }
}
