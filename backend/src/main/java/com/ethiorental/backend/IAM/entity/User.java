package com.ethiorental.backend.IAM.entity;

import jakarta.persistence.*;

import org.antlr.v4.runtime.misc.NotNull;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.processing.Pattern;
import org.hibernate.type.SqlTypes;
import jakarta.validation.constraints.NotBlank;


import com.ethiorental.backend.IAM.enums.Gender;

import java.time.LocalDate;
import java.util.UUID;

@Entity
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID id;

    @NotNull
    @Column(nullable = false, unique = true)
    private Long fayda_id;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String middelName;

    @Column(nullable = false)
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender;

    @NotNull
    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @NotBlank
    @Pattern(regexp = "^\\+?[0-9]{9,15}$", message = "Invalid phone number")
    @Column(nullable = false, unique = true)
    private String phoneNumber;

    @NotBlank
    @Email(message = "Invalid email address")
    @Column(nullable = false, unique = true)
    private String email;
}