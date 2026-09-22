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
    /** Location fields for citizens */
    private String city;
    /** Jurisdiction — only populated for GOVERNMENT_EMPLOYEE */
    private String subCity;
    private String woreda;

    /** National ID / Fayda ID */
    private String nationalId;
    /** House number */
    private String houseNumber;
    /** Taxpayer Identification Number */
    private String tinNumber;
    /** Emergency contact person name */
    private String emergencyContactName;
    /** Emergency contact person phone number */
    private String emergencyContactPhone;

    /** Landlord Payout Settings (Account 1 - Primary) */
    private String preferredPaymentMethod;
    private String bankName;
    private String accountNumber;
    private String accountHolderName;

    /** Landlord Payout Settings (Account 2 - Secondary) */
    private String bankName2;
    private String accountNumber2;
    private String accountHolderName2;

    /** Landlord Payout Settings (Account 3 - Tertiary) */
    private String bankName3;
    private String accountNumber3;
    private String accountHolderName3;
}
