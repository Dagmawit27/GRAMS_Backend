package com.ethiorental.backend.IAM.dto.request;

import com.ethiorental.backend.IAM.enums.Gender;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class RegisterEmployeeRequest {

    @NotBlank(message = "Employee number is required")
    private String employeeNumber;

    @NotNull(message = "Office ID is required")
    private UUID officeId;

    @NotBlank(message = "First name is required")
    private String firstName;

    private String middleName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotNull(message = "Gender is required")
    private Gender gender;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+?[0-9]{9,15}$", message = "Invalid phone number format")
    private String phoneNumber;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Position title is required")
    private String positionTitle;

    // Optional: if not provided, default password is "Change@{employeeNumber}"
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    @NotEmpty(message = "At least one role must be assigned")
    private List<String> roles;
}
