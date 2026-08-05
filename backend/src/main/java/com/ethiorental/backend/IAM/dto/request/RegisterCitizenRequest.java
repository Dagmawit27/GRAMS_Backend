package com.ethiorental.backend.IAM.dto.request;

import com.ethiorental.backend.IAM.enums.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class RegisterCitizenRequest {

    @NotNull(message = "Fayda ID is required")
    private Long faydaId;

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Middle name is required")
    private String middleName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotNull(message = "Gender is required (MALE or FEMALE)")
    private Gender gender;

    @NotNull(message = "Date of birth is required")
    private LocalDate dateOfBirth;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+?[0-9]{9,15}$", message = "Invalid phone number format")
    private String phoneNumber;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    // Optional role preference: LANDLORD, TENANT, or BOTH (default CITIZEN)
    private String rolePreference;
}
