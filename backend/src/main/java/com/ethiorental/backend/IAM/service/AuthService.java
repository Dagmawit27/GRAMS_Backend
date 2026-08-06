package com.ethiorental.backend.IAM.service;

import com.ethiorental.backend.IAM.dto.request.LoginRequest;
import com.ethiorental.backend.IAM.dto.request.RegisterCitizenRequest;
import com.ethiorental.backend.IAM.dto.request.RegisterEmployeeRequest;
import com.ethiorental.backend.IAM.dto.request.RefreshTokenRequest;
import com.ethiorental.backend.IAM.dto.response.AuthResponse;
import com.ethiorental.backend.IAM.dto.response.UserSummaryDto;
import com.ethiorental.backend.IAM.entity.*;
import com.ethiorental.backend.IAM.enums.CitizenStatus;
import com.ethiorental.backend.IAM.enums.EmployeeStatus;
import com.ethiorental.backend.IAM.repository.*;
import com.ethiorental.backend.IAM.security.CustomUserDetailsService;
import com.ethiorental.backend.IAM.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final CitizenRepository citizenRepository;
    private final CitizenCredentialRepository citizenCredentialRepository;
    private final CitizenRoleRepository citizenRoleRepository;
    private final GovernmentEmployeeRepository employeeRepository;
    private final EmployeeCredentialRepository employeeCredentialRepository;
    private final EmployeeRoleRepository employeeRoleRepository;
    private final RoleRepository roleRepository;
    private final OfficeRepository officeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;

    // ── Citizen Registration ─────────────────────────────────────────────────

    @Transactional
    public AuthResponse registerCitizen(RegisterCitizenRequest req) {
        if (citizenCredentialRepository.existsByUsername(req.getEmail()))
            throw new IllegalArgumentException("Email already registered.");
        if (citizenRepository.existsByFaydaId(String.valueOf(req.getFaydaId())))
            throw new IllegalArgumentException("Fayda ID already registered.");
        if (citizenRepository.existsByPhone(req.getPhoneNumber()))
            throw new IllegalArgumentException("Phone number already registered.");

        Citizen citizen = Citizen.builder()
                .faydaId(String.valueOf(req.getFaydaId()))
                .firstName(req.getFirstName())
                .middleName(req.getMiddleName())
                .lastName(req.getLastName())
                .gender(req.getGender())
                .dateOfBirth(req.getDateOfBirth())
                .phone(req.getPhoneNumber())
                .email(req.getEmail())
                .status(CitizenStatus.ACTIVE)
                .build();
        citizenRepository.save(citizen);

        CitizenCredential credential = CitizenCredential.builder()
                .citizen(citizen)
                .username(req.getEmail())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .build();
        citizenCredentialRepository.save(credential);

        // Assign role: LANDLORD, TENANT, or default CITIZEN
        String roleName = resolveRolePreference(req.getRolePreference());
        roleRepository.findByRoleName(roleName).ifPresent(role -> {
            citizenRoleRepository.save(CitizenRole.builder()
                    .citizen(citizen).role(role).build());
        });

        UserDetails userDetails = userDetailsService.loadUserByUsername(req.getEmail());
        List<String> roles = userDetails.getAuthorities().stream()
                .map(a -> a.getAuthority().replace("ROLE_", "")).toList();

        String token = jwtUtils.generateAccessToken(req.getEmail(), roles);
        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(jwtUtils.getJwtExpirationMs())
                .user(buildCitizenSummary(citizen, roles))
                .build();
    }

    // ── Citizen Login ────────────────────────────────────────────────────────

    @Transactional
    public AuthResponse loginCitizen(LoginRequest req) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getLoginIdentifier(), req.getPassword())
        );
        UserDetails userDetails = (UserDetails) auth.getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(a -> a.getAuthority().replace("ROLE_", "")).toList();

        // Update last login
        citizenCredentialRepository.findByUsername(userDetails.getUsername()).ifPresent(c -> {
            c.setLastLogin(LocalDateTime.now());
            citizenCredentialRepository.save(c);
        });

        CitizenCredential cred = citizenCredentialRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        String token = jwtUtils.generateAccessToken(userDetails.getUsername(), roles);
        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(jwtUtils.getJwtExpirationMs())
                .user(buildCitizenSummary(cred.getCitizen(), roles))
                .build();
    }

    // ── Employee Login ───────────────────────────────────────────────────────

    @Transactional
    public AuthResponse loginEmployee(LoginRequest req) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getLoginIdentifier(), req.getPassword())
        );
        UserDetails userDetails = (UserDetails) auth.getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(a -> a.getAuthority().replace("ROLE_", "")).toList();

        employeeCredentialRepository.findByUsername(userDetails.getUsername()).ifPresent(c -> {
            c.setLastLogin(LocalDateTime.now());
            employeeCredentialRepository.save(c);
        });

        EmployeeCredential cred = employeeCredentialRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("Employee not found."));

        String token = jwtUtils.generateAccessToken(userDetails.getUsername(), roles);
        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(jwtUtils.getJwtExpirationMs())
                .user(buildEmployeeSummary(cred.getEmployee(), roles))
                .build();
    }

    // ── Employee Registration (SYSTEM_ADMINISTRATOR only) ───────────────────

    @Transactional
    public AuthResponse registerEmployee(RegisterEmployeeRequest req) {
        if (employeeRepository.existsByEmployeeNumber(req.getEmployeeNumber()))
            throw new IllegalArgumentException("Employee number already registered.");
        if (employeeRepository.existsByEmail(req.getEmail()))
            throw new IllegalArgumentException("Email already registered.");
        if (employeeRepository.existsByPhone(req.getPhoneNumber()))
            throw new IllegalArgumentException("Phone number already registered.");
        if (employeeCredentialRepository.existsByUsername(req.getEmail()))
            throw new IllegalArgumentException("Username already taken.");

        // Require a valid office
        Office office = officeRepository.findById(req.getOfficeId())
                .orElseThrow(() -> new IllegalArgumentException("Office not found: " + req.getOfficeId()));

        GovernmentEmployee employee = GovernmentEmployee.builder()
                .employeeNumber(req.getEmployeeNumber())
                .office(office)
                .firstName(req.getFirstName())
                .middleName(req.getMiddleName())
                .lastName(req.getLastName())
                .gender(req.getGender())
                .phone(req.getPhoneNumber())
                .email(req.getEmail())
                .position(req.getPositionTitle())
                .status(EmployeeStatus.ACTIVE)
                .build();
        employeeRepository.save(employee);

        // Default password set by admin
        String defaultPassword = req.getPassword() != null ? req.getPassword()
                : "Change@" + req.getEmployeeNumber();

        EmployeeCredential credential = EmployeeCredential.builder()
                .employee(employee)
                .username(req.getEmail())
                .passwordHash(passwordEncoder.encode(defaultPassword))
                .build();
        employeeCredentialRepository.save(credential);

        // Assign roles
        for (String roleName : req.getRoles()) {
            roleRepository.findByRoleName(roleName).ifPresent(role ->
                    employeeRoleRepository.save(EmployeeRole.builder()
                            .employee(employee).role(role).build())
            );
        }

        List<String> roles = req.getRoles();
        String token = jwtUtils.generateAccessToken(req.getEmail(), roles);
        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(jwtUtils.getJwtExpirationMs())
                .user(buildEmployeeSummary(employee, roles))
                .build();
    }

    // ── Refresh token (stub — stateless, just reissue) ───────────────────────

    public AuthResponse refreshToken(RefreshTokenRequest req) {
        // Stateless: validate and reissue
        if (!jwtUtils.validateJwtToken(req.getRefreshToken()))
            throw new IllegalArgumentException("Invalid or expired refresh token.");
        String username = jwtUtils.getSubjectFromJwt(req.getRefreshToken());
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        List<String> roles = userDetails.getAuthorities().stream()
                .map(a -> a.getAuthority().replace("ROLE_", "")).toList();
        String newToken = jwtUtils.generateAccessToken(username, roles);
        return AuthResponse.builder()
                .accessToken(newToken)
                .tokenType("Bearer")
                .expiresIn(jwtUtils.getJwtExpirationMs())
                .build();
    }

    public void logout(String refreshToken) {
        // Stateless JWT — nothing to invalidate server-side
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    public UserSummaryDto buildUserSummary(String username) {
        // Try employee first
        EmployeeCredential empCred = employeeCredentialRepository.findByUsername(username).orElse(null);
        if (empCred != null) {
            List<String> roles = employeeRoleRepository.findByEmployee(empCred.getEmployee())
                    .stream().map(r -> r.getRole().getRoleName()).toList();
            return buildEmployeeSummary(empCred.getEmployee(), roles);
        }
        CitizenCredential citizenCred = citizenCredentialRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
        List<String> roles = citizenRoleRepository.findByCitizen(citizenCred.getCitizen())
                .stream().map(r -> r.getRole().getRoleName()).toList();
        return buildCitizenSummary(citizenCred.getCitizen(), roles);
    }

    private UserSummaryDto buildCitizenSummary(Citizen c, List<String> roles) {
        return UserSummaryDto.builder()
                .id(c.getId())
                .firstName(c.getFirstName())
                .middleName(c.getMiddleName())
                .lastName(c.getLastName())
                .gender(c.getGender())
                .dateOfBirth(c.getDateOfBirth())
                .phoneNumber(c.getPhone())
                .email(c.getEmail())
                .createdAt(c.getCreatedAt())
                .roles(roles)
                .userType("CITIZEN")
                .governmentEmployee(false)
                .build();
    }

    private UserSummaryDto buildEmployeeSummary(GovernmentEmployee e, List<String> roles) {
        return UserSummaryDto.builder()
                .id(e.getId())
                .firstName(e.getFirstName())
                .middleName(e.getMiddleName())
                .lastName(e.getLastName())
                .gender(e.getGender())
                .phoneNumber(e.getPhone())
                .email(e.getEmail())
                .createdAt(e.getCreatedAt())
                .roles(roles)
                .userType("GOVERNMENT_EMPLOYEE")
                .governmentEmployee(true)
                .employeeNumber(e.getEmployeeNumber())
                .positionTitle(e.getPosition())
                .build();
    }

    private String resolveRolePreference(String pref) {
        if (pref == null) return "CITIZEN";
        return switch (pref.toUpperCase()) {
            case "LANDLORD" -> "LANDLORD";
            case "TENANT" -> "TENANT";
            default -> "CITIZEN";
        };
    }
}
