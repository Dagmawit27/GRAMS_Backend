package com.ethiorental.backend.IAM.entity;

import com.ethiorental.backend.IAM.enums.CitizenStatus;
import com.ethiorental.backend.IAM.enums.Gender;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "citizens")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Citizen {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(nullable = false)
    private String firstName;

    private String middleName;

    @Column(nullable = false)
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender;

    @Column(nullable = false)
    private LocalDate dateOfBirth;

    @Column(nullable = false, unique = true)
    private String phone;

    @Column(nullable = false, unique = true)
    private String email;

    /** National ID / Fayda ID of the citizen */
    private String nationalId;

    /** Employer / organization the citizen works at (e.g. "CBE", "Ethio Telecom") */
    private String worksOn;

    /** Location fields for agreement form */
    private String region;
    private String city;
    private String subCity;
    private String woreda;
    private String houseNumber;
    private String specificPlace;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CitizenStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) this.status = CitizenStatus.ACTIVE;
    }

    public String getFullName() {
        if (middleName != null && !middleName.isEmpty()) {
            return firstName + " " + middleName + " " + lastName;
        }
        return firstName + " " + lastName;
    }

    public String getPhoneNumber() {
        return phone;
    }
}
