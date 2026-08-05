package com.ethiorental.backend.IAM.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.ethiorental.backend.IAM.enums.Gender;
import com.ethiorental.backend.IAM.enums.Status;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSummaryDto {
    private UUID id;
    private Long faydaId;
    private String firstName;
    private String middleName;
    private String lastName;
    private Gender gender;
    private LocalDate dateOfBirth;
    private String phoneNumber;
    private String email;
    private Status status;
    private LocalDateTime createdAt;

    private List<String> roles;

    private String userType; // "GOVERNMENT_EMPLOYEE" or "CITIZEN"

    private boolean governmentEmployee;
    private String employeeNumber;
    private String department;
    private String woredaCode;
    private String subCityCode;
    private String positionTitle;
}
