package com.ethiorental.backend.IAM.security;

import com.ethiorental.backend.IAM.entity.CitizenCredential;
import com.ethiorental.backend.IAM.entity.EmployeeCredential;
import com.ethiorental.backend.IAM.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final CitizenCredentialRepository citizenCredentialRepository;
    private final EmployeeCredentialRepository employeeCredentialRepository;
    private final CitizenRoleRepository citizenRoleRepository;
    private final EmployeeRoleRepository employeeRoleRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // Try employee credentials first
        Optional<EmployeeCredential> empCred = employeeCredentialRepository.findByEmail(email);
        if (empCred.isPresent()) {
            EmployeeCredential cred = empCred.get();
            List<SimpleGrantedAuthority> authorities = employeeRoleRepository
                    .findByEmployee(cred.getEmployee())
                    .stream()
                    .map(er -> {
                        String name = er.getRole().getRoleName();
                        return new SimpleGrantedAuthority(name.startsWith("ROLE_") ? name : "ROLE_" + name);
                    })
                    .toList();
            return new User(cred.getEmail(), cred.getPasswordHash(), authorities);
        }

        // Try citizen credentials
        Optional<CitizenCredential> citizenCred = citizenCredentialRepository.findByEmail(email);
        if (citizenCred.isPresent()) {
            CitizenCredential cred = citizenCred.get();
            List<SimpleGrantedAuthority> authorities = citizenRoleRepository
                    .findByCitizen(cred.getCitizen())
                    .stream()
                    .map(cr -> {
                        String name = cr.getRole().getRoleName();
                        return new SimpleGrantedAuthority(name.startsWith("ROLE_") ? name : "ROLE_" + name);
                    })
                    .toList();
            if (authorities.isEmpty()) {
                authorities = List.of(new SimpleGrantedAuthority("ROLE_CITIZEN"));
            }
            return new User(cred.getEmail(), cred.getPasswordHash(), authorities);
        }

        throw new UsernameNotFoundException("User not found: " + email);
    }
}
