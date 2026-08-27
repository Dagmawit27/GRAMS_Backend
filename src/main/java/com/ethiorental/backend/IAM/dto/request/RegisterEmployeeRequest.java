package com.ethiorental.backend.IAM.dto.request;

import com.ethiorental.backend.IAM.enums.Gender;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

@Data
public class RegisterEmployeeRequest {

    @NotBlank(message = "Employee number is required")
    private String employeeNumber;

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

    /** Sub-city the employee's office belongs to (e.g. "Akaky Kaliti") */
    @NotBlank(message = "Sub-city is required")
    private String subCity;

    /** Woreda number (e.g. "03") */
    @NotBlank(message = "Woreda is required")
    private String woreda;

    @NotBlank(message = "Office type is required")
    private String officeType;

    // Optional: if not provided, default password is "Change@{employeeNumber}"
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    @NotEmpty(message = "At least one role must be assigned")
    private List<String> roles;
}
