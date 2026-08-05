package com.ethiorental.backend.IAM.service;

import com.ethiorental.backend.IAM.adapter.FaydaAdapterService;
import com.ethiorental.backend.IAM.dto.request.LoginRequest;
import com.ethiorental.backend.IAM.dto.request.RefreshTokenRequest;
import com.ethiorental.backend.IAM.dto.request.RegisterCitizenRequest;
import com.ethiorental.backend.IAM.dto.request.RegisterEmployeeRequest;
import com.ethiorental.backend.IAM.dto.response.AuthResponse;
import com.ethiorental.backend.IAM.dto.response.FaydaCitizenResponseDto;
import com.ethiorental.backend.IAM.dto.response.UserSummaryDto;
import com.ethiorental.backend.IAM.entity.*;
import com.ethiorental.backend.IAM.enums.Status;
import com.ethiorental.backend.IAM.repository.*;
import com.ethiorental.backend.IAM.security.CustomUserDetailsService;
import com.ethiorental.backend.IAM.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final GovernmentEmployeeRepository governmentEmployeeRepository;
    private final EmployeeRoleRepository employeeRoleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final FaydaAdapterService faydaAdapterService;
    private final CustomUserDetailsService userDetailsService;
    private final JwtUtils jwtUtils;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public AuthResponse registerCitizen(RegisterCitizenRequest request) {
        if (userRepository.existsByFaydaId(request.getFaydaId())) {
            throw new IllegalArgumentException("Citizen with Fayda ID " + request.getFaydaId() + " is already registered.");
        }

        // 1. Verify Fayda ID status via Fayda Adapter
        FaydaCitizenResponseDto faydaIdentity = faydaAdapterService.fetchCitizenIdentity(request.getFaydaId());
        if (!faydaIdentity.isVerified()) {
            throw new IllegalArgumentException("Fayda identity verification failed: " + faydaIdentity.getStatusMessage());
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email " + request.getEmail() + " is already registered.");
        }
        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new IllegalArgumentException("Phone number " + request.getPhoneNumber() + " is already registered.");
        }

        // 2. Create User entity with user-supplied details
        User user = new User();
        user.setFaydaId(request.getFaydaId());
        user.setFirstName(request.getFirstName());
        user.setMiddleName(request.getMiddleName());
        user.setLastName(request.getLastName());
        user.setGender(request.getGender());
        user.setDateOfBirth(request.getDateOfBirth());
        user.setEmail(request.getEmail());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setStatus(Status.ACTIVE);

        user = userRepository.save(user);

        // 3. Assign LANDLORD and/or TENANT roles (default BOTH if no role is entered)
        String pref = (request.getRolePreference() != null && !request.getRolePreference().isBlank())
                ? request.getRolePreference().trim().toUpperCase()
                : "BOTH";

        if ("LANDLORD".equals(pref)) {
            assignRoleToUser(user, "LANDLORD");
        } else if ("TENANT".equals(pref)) {
            assignRoleToUser(user, "TENANT");
        } else {
            assignRoleToUser(user, "LANDLORD");
            assignRoleToUser(user, "TENANT");
        }

        return createAuthResponse(user);
    }

    @Transactional
    public AuthResponse registerEmployee(RegisterEmployeeRequest request) {
        if (governmentEmployeeRepository.existsByEmployeeNumber(request.getEmployeeNumber())) {
            throw new IllegalArgumentException("Employee number " + request.getEmployeeNumber() + " is already registered.");
        }

        User user;
        if (userRepository.existsByFaydaId(request.getFaydaId())) {
            user = userRepository.findByFaydaId(request.getFaydaId()).orElseThrow();
        } else {
            FaydaCitizenResponseDto faydaIdentity = faydaAdapterService.fetchCitizenIdentity(request.getFaydaId());
            if (!faydaIdentity.isVerified()) {
                throw new IllegalArgumentException("Fayda identity verification failed for employee registration.");
            }
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new IllegalArgumentException("Email " + request.getEmail() + " is already registered.");
            }
            if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
                throw new IllegalArgumentException("Phone number " + request.getPhoneNumber() + " is already registered.");
            }

            user = new User();
            user.setFaydaId(request.getFaydaId());
            user.setFirstName(request.getFirstName());
            user.setMiddleName(request.getMiddleName());
            user.setLastName(request.getLastName());
            user.setGender(request.getGender());
            user.setDateOfBirth(request.getDateOfBirth());
            user.setEmail(request.getEmail());
            user.setPhoneNumber(request.getPhoneNumber());
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            user.setStatus(Status.ACTIVE);
            user = userRepository.save(user);

            assignRoleToUser(user, "LANDLORD");
            assignRoleToUser(user, "TENANT");
        }

        // Create GovernmentEmployee entity
        GovernmentEmployee employee = new GovernmentEmployee();
        employee.setUser(user);
        employee.setEmployeeNumber(request.getEmployeeNumber());
        employee.setDepartment(request.getDepartment());
        employee.setWoredaCode(request.getWoredaCode());
        employee.setSubCityCode(request.getSubCityCode());
        employee.setPositionTitle(request.getPositionTitle());
        employee.setStatus(Status.ACTIVE);
        employee = governmentEmployeeRepository.save(employee);

        // Assign employee roles to employee_roles table using the same shared Role entities
        for (String roleName : request.getRoles()) {
            assignRoleToEmployee(employee, roleName.toUpperCase());
        }

        return createAuthResponse(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userDetailsService.findUserByIdentifier(request.getLoginIdentifier())
                .orElseThrow(() -> new IllegalArgumentException("Invalid login identifier or password."));

        if (user.getLockoutExpiration() != null && LocalDateTime.now().isBefore(user.getLockoutExpiration())) {
            throw new IllegalStateException("Account is temporarily locked due to multiple failed login attempts. Try again later.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            int failedAttempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(failedAttempts);
            if (failedAttempts >= 5) {
                user.setLockoutExpiration(LocalDateTime.now().plusMinutes(15));
            }
            userRepository.save(user);
            throw new IllegalArgumentException("Invalid login identifier or password.");
        }

        // Reset failed login attempts on success
        if (user.getFailedLoginAttempts() > 0 || user.getLockoutExpiration() != null) {
            user.setFailedLoginAttempts(0);
            user.setLockoutExpiration(null);
            userRepository.save(user);
        }

        return createAuthResponse(user);
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token."));

        if (refreshToken.isRevoked() || refreshToken.getExpiryDate().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Refresh token is expired or revoked.");
        }

        return createAuthResponse(refreshToken.getUser());
    }

    @Transactional
    public void logout(String token) {
        if (token != null && !token.isBlank()) {
            refreshTokenRepository.deleteByToken(token);
        }
    }

    private void assignRoleToUser(User user, String roleName) {
        Role role = roleRepository.findByRoleName(roleName)
                .orElseGet(() -> {
                    Role r = new Role();
                    r.setRoleName(roleName);
                    return roleRepository.save(r);
                });

        if (!userRoleRepository.existsByUserAndRole(user, role)) {
            UserRole userRole = new UserRole();
            userRole.setUser(user);
            userRole.setRole(role);
            userRoleRepository.save(userRole);
        }
    }

    private void assignRoleToEmployee(GovernmentEmployee employee, String roleName) {
        Role role = roleRepository.findByRoleName(roleName)
                .orElseGet(() -> {
                    Role r = new Role();
                    r.setRoleName(roleName);
                    return roleRepository.save(r);
                });

        if (!employeeRoleRepository.existsByEmployeeAndRole(employee, role)) {
            EmployeeRole employeeRole = new EmployeeRole();
            employeeRole.setEmployee(employee);
            employeeRole.setRole(role);
            employeeRoleRepository.save(employeeRole);
        }
    }

    private AuthResponse createAuthResponse(User user) {
        List<String> roles = userDetailsService.extractRolesForUser(user);
        String accessToken = jwtUtils.generateAccessToken(user.getEmail(), roles);

        // Generate Refresh Token (stored in DB)
        refreshTokenRepository.deleteByUser(user);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setExpiryDate(Instant.now().plusSeconds(7 * 24 * 3600)); // 7 days
        refreshToken.setRevoked(false);
        refreshTokenRepository.save(refreshToken);

        UserSummaryDto userSummary = buildUserSummary(user, roles);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .expiresIn(jwtUtils.getJwtExpirationMs() / 1000)
                .user(userSummary)
                .build();
    }

    public UserSummaryDto buildUserSummary(User user, List<String> roles) {
        UserSummaryDto.UserSummaryDtoBuilder builder = UserSummaryDto.builder()
                .id(user.getId())
                .faydaId(user.getFaydaId())
                .firstName(user.getFirstName())
                .middleName(user.getMiddleName())
                .lastName(user.getLastName())
                .gender(user.getGender())
                .dateOfBirth(user.getDateOfBirth())
                .phoneNumber(user.getPhoneNumber())
                .email(user.getEmail())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .roles(roles);

        governmentEmployeeRepository.findByUser(user).ifPresent(emp -> {
            builder.governmentEmployee(true)
                    .employeeNumber(emp.getEmployeeNumber())
                    .department(emp.getDepartment())
                    .woredaCode(emp.getWoredaCode())
                    .subCityCode(emp.getSubCityCode())
                    .positionTitle(emp.getPositionTitle());
        });

        return builder.build();
    }
}
