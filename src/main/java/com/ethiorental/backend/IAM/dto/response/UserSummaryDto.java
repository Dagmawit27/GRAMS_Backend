package com.ethiorental.backend.IAM.dto.response;

import com.ethiorental.backend.IAM.enums.Gender;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSummaryDto {
    private UUID id;
    private String firstName;
    private String middleName;
    private String lastName;
    private Gender gender;
    private LocalDate dateOfBirth;
    private String phoneNumber;
    private String email;
    private LocalDateTime createdAt;
    private List<String> roles;
    private String userType;           // "CITIZEN" or "GOVERNMENT_EMPLOYEE"
    private boolean governmentEmployee;
    private String employeeNumber;
    private String positionTitle;
    private String worksOn;
    /** Jurisdiction — only populated for GOVERNMENT_EMPLOYEE */
    private String subCity;
    private String woreda;
}
