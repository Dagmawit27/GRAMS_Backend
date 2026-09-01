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
import com.ethiorental.backend.location.repository.SubCityWoredaRepository;
import com.ethiorental.backend.shared.audit.AuditAction;
import com.ethiorental.backend.shared.audit.AuditEventRequest;
import com.ethiorental.backend.shared.audit.AuditOutcome;
import com.ethiorental.backend.shared.audit.AuditService;
import com.ethiorental.backend.shared.audit.Auditable;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
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
    private final SubCityWoredaRepository subCityWoredaRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;
    private final AuditService auditService;

    // ── Citizen Registration ─────────────────────────────────────────────────

    @Transactional
    public AuthResponse registerCitizen(RegisterCitizenRequest req) {
        try {
            if (citizenCredentialRepository.existsByEmail(req.getEmail()))
                throw new IllegalArgumentException("Email already registered.");
            if (citizenRepository.existsByPhone(req.getPhoneNumber()))
                throw new IllegalArgumentException("Phone number already registered.");

            Citizen citizen = Citizen.builder()
                    .firstName(req.getFirstName())
                    .middleName(req.getMiddleName())
                    .lastName(req.getLastName())
                    .gender(req.getGender())
                    .dateOfBirth(req.getDateOfBirth())
                    .phone(req.getPhoneNumber())
                    .email(req.getEmail())
                    .worksOn(req.getWorksOn())
                    .region(req.getRegion())
                    .city(req.getCity())
                    .subCity(req.getSubCity())
                    .woreda(req.getWoreda())
                    .houseNumber(req.getHouseNumber())
                    .specificPlace(req.getSpecificPlace())
                    .status(CitizenStatus.ACTIVE)
                    .build();
            citizenRepository.save(citizen);

            CitizenCredential credential = CitizenCredential.builder()
                    .citizen(citizen)
                    .email(req.getEmail())
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

            auditService.log(new AuditEventRequest(
                    AuditAction.REGISTER,
                    "IAM",
                    citizen.getId().toString(),
                    null,
                    null,
                    AuditOutcome.SUCCESS,
                    "Citizen registration completed",
                    null
            ));

            String token = jwtUtils.generateAccessToken(req.getEmail(), roles);
            return AuthResponse.builder()
                    .accessToken(token)
                    .tokenType("Bearer")
                    .expiresIn(jwtUtils.getJwtExpirationMs())
                    .user(buildCitizenSummary(citizen, roles))
                    .build();
        } catch (RuntimeException ex) {
            auditService.log(new AuditEventRequest(
                    AuditAction.REGISTER,
                    "IAM",
                    req != null ? req.getEmail() : null,
                    null,
                    null,
                    AuditOutcome.FAILURE,
                    ex.getMessage(),
                    null
            ));
            throw ex;
        }
    }

    // ── Citizen Login ────────────────────────────────────────────────────────

    // SRS §5.10, BR-027 — log successful login; failed logins are caught and logged below
    @Transactional
    public AuthResponse loginCitizen(LoginRequest req) {
        Authentication auth;
        try {
            auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword())
            );
        } catch (BadCredentialsException ex) {
            auditService.log(new AuditEventRequest(
                    AuditAction.LOGIN_FAILED,
                    "IAM",
                    req.getEmail(),
                    null, null,
                    AuditOutcome.FAILURE,
                    "Bad credentials for citizen login",
                    null
            ));
            throw ex;
        }
        UserDetails userDetails = (UserDetails) auth.getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(a -> a.getAuthority().replace("ROLE_", "")).toList();

        citizenCredentialRepository.findByEmail(userDetails.getUsername()).ifPresent(c -> {
            c.setLastLogin(LocalDateTime.now());
            citizenCredentialRepository.save(c);
        });

        CitizenCredential cred = citizenCredentialRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        auditService.log(new AuditEventRequest(
                AuditAction.LOGIN,
                "IAM",
                cred.getCitizen().getId().toString(),
                null, null,
                AuditOutcome.SUCCESS,
                null,
                null
        ));

        String token = jwtUtils.generateAccessToken(userDetails.getUsername(), roles);
        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(jwtUtils.getJwtExpirationMs())
                .user(buildCitizenSummary(cred.getCitizen(), roles))
                .build();
    }

    // ── Employee Login ───────────────────────────────────────────────────────

    // SRS §5.10, BR-027 — log employee login events
    @Transactional
    public AuthResponse loginEmployee(LoginRequest req) {
        Authentication auth;
        try {
            auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword())
            );
        } catch (BadCredentialsException ex) {
            auditService.log(new AuditEventRequest(
                    AuditAction.LOGIN_FAILED,
                    "IAM",
                    req.getEmail(),
                    null, null,
                    AuditOutcome.FAILURE,
                    "Bad credentials for employee login",
                    null
            ));
            throw ex;
        }
        UserDetails userDetails = (UserDetails) auth.getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(a -> a.getAuthority().replace("ROLE_", "")).toList();

        employeeCredentialRepository.findByEmail(userDetails.getUsername()).ifPresent(c -> {
            c.setLastLogin(LocalDateTime.now());
            employeeCredentialRepository.save(c);
        });

        EmployeeCredential cred = employeeCredentialRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("Employee not found."));

        auditService.log(new AuditEventRequest(
                AuditAction.LOGIN,
                "IAM",
                cred.getEmployee().getId().toString(),
                null, null,
                AuditOutcome.SUCCESS,
                null,
                null
        ));

        String token = jwtUtils.generateAccessToken(userDetails.getUsername(), roles);
        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(jwtUtils.getJwtExpirationMs())
                .user(buildEmployeeSummary(cred.getEmployee(), roles))
                .build();
    }

    // ── Employee Registration (SYSTEM_ADMINISTRATOR only) ───────────────────

    // SRS §5.10, BR-027 — log government employee registration
    @Auditable(action = AuditAction.REGISTER, module = "IAM")
    @Transactional
    public AuthResponse registerEmployee(RegisterEmployeeRequest req) {
        // Validate that the sub-city + woreda exists in the reference table
        if (!subCityWoredaRepository.existsBySubCityIgnoreCaseAndWoreda(req.getSubCity(), req.getWoreda())) {
            throw new IllegalArgumentException(
                "Invalid jurisdiction: '" + req.getSubCity() + "' Woreda " + req.getWoreda()
                + " is not a recognised Addis Ababa sub-city/woreda combination."
            );
        }

        if (employeeRepository.existsByEmployeeNumber(req.getEmployeeNumber()))
            throw new IllegalArgumentException("Employee number already registered.");
        if (employeeRepository.existsByEmail(req.getEmail()))
            throw new IllegalArgumentException("Email already registered.");
        if (employeeRepository.existsByPhone(req.getPhoneNumber()))
            throw new IllegalArgumentException("Phone number already registered.");
        if (employeeCredentialRepository.existsByEmail(req.getEmail()))
            throw new IllegalArgumentException("Username already taken.");

        // Resolve or create the office for this sub-city + woreda
        Office office = officeRepository
                .findBySubCityIgnoreCaseAndWoreda(req.getSubCity(), req.getWoreda())
                .orElseGet(() -> officeRepository.save(
                        Office.builder()
                                .officeName(req.getSubCity() + " Sub-City Woreda " + req.getWoreda() + " Housing Desk")
                                .officeType(req.getOfficeType() != null ? req.getOfficeType() : "WOREDA_OFFICE")
                                .subCity(req.getSubCity())
                                .woreda(req.getWoreda())
                                .build()
                ));

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
                .email(req.getEmail())
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

    // SRS §5.10, BR-027 — record logout events
    public void logout(String refreshToken) {
        // Stateless JWT — nothing to invalidate server-side, but we still audit the event
        auditService.log(new AuditEventRequest(
                AuditAction.LOGOUT,
                "IAM",
                null,
                null, null,
                AuditOutcome.SUCCESS,
                null,
                null
        ));
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    public UserSummaryDto buildUserSummary(String email) {
        EmployeeCredential empCred = employeeCredentialRepository.findByEmail(email).orElse(null);
        if (empCred != null) {
            List<String> roles = employeeRoleRepository.findByEmployee(empCred.getEmployee())
                    .stream().map(r -> r.getRole().getRoleName()).toList();
            return buildEmployeeSummary(empCred.getEmployee(), roles);
        }
        CitizenCredential citizenCred = citizenCredentialRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + email));
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
                .worksOn(c.getWorksOn())
                .createdAt(c.getCreatedAt())
                .roles(roles)
                .userType("CITIZEN")
                .governmentEmployee(false)
                .build();
    }

    private UserSummaryDto buildEmployeeSummary(GovernmentEmployee e, List<String> roles) {
        String subCity = e.getOffice() != null ? e.getOffice().getSubCity() : null;
        String woreda  = e.getOffice() != null ? e.getOffice().getWoreda()  : null;
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
                .subCity(subCity)
                .woreda(woreda)
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
