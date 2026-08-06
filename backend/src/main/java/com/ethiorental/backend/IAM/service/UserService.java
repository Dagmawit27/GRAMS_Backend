package com.ethiorental.backend.IAM.service;

import com.ethiorental.backend.IAM.dto.response.UserSummaryDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final AuthService authService;

    public UserSummaryDto getCurrentUserProfile(String username) {
        return authService.buildUserSummary(username);
    }
}
