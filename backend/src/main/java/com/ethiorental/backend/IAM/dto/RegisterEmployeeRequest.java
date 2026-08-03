package com.ethiorental.backend.IAM.dto;

import com.ethiorental.backend.IAM.enums.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class RegisterEmployeeRequest {

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
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    @NotBlank(message = "Employee number is required")
    private String employeeNumber;

    @NotBlank(message = "Department is required")
    private String department;

    private String woredaCode;
    private String subCityCode;

    @NotBlank(message = "Position title is required")
    private String positionTitle;

    @NotEmpty(message = "At least one employee role must be specified")
    private List<String> roles;
}
