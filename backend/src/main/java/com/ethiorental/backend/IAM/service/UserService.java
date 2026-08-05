package com.ethiorental.backend.IAM.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ethiorental.backend.IAM.dto.UserSummaryDto;
import com.ethiorental.backend.IAM.entity.User;
import com.ethiorental.backend.IAM.repository.UserRepository;
import com.ethiorental.backend.IAM.security.CustomUserDetailsService;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final AuthService authService;
    private final CustomUserDetailsService userDetailsService;

    @Transactional(readOnly = true)
    public UserSummaryDto getUserProfileById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + id));

        List<String> roles = userDetailsService.extractRolesForUser(user);
        return authService.buildUserSummary(user, roles);
    }

    @Transactional(readOnly = true)
    public UserSummaryDto getUserProfileByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email));

        List<String> roles = userDetailsService.extractRolesForUser(user);
        return authService.buildUserSummary(user, roles);
    }
}
