package com.ethiorental.backend.IAM.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

import com.ethiorental.backend.IAM.enums.Gender;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FaydaCitizenResponseDto {
    private Long faydaId;
    private String firstName;
    private String middleName;
    private String lastName;
    private Gender gender;
    private LocalDate dateOfBirth;
    private String phoneNumber;
    private String email;
    private boolean verified;
    private String statusMessage;
}
