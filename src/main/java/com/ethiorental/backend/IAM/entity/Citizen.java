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

    /** City where the citizen resides */
    private String city;

    /** Sub-city where the citizen resides */
    private String subCity;

    /** Woreda where the citizen resides */
    private String woreda;

    /** House number in Kebele */
    private String houseNumber;

    /** Taxpayer Identification Number (TIN) */
    private String tinNumber;

    /** Emergency contact person name */
    private String emergencyContactName;

    /** Emergency contact person phone number */
    private String emergencyContactPhone;

    /** Preferred payment method / active primary account key */
    private String preferredPaymentMethod;

    /** Primary Designated payout bank name (Account 1) */
    private String bankName;

    /** Primary Payout bank or mobile money account number (Account 1) */
    private String accountNumber;

    /** Primary Full legal name on bank / mobile money account (Account 1) */
    private String accountHolderName;

    /** Secondary payout bank name (Account 2) */
    private String bankName2;

    /** Secondary payout account number (Account 2) */
    private String accountNumber2;

    /** Secondary account holder name (Account 2) */
    private String accountHolderName2;

    /** Tertiary payout bank name (Account 3) */
    private String bankName3;

    /** Tertiary payout account number (Account 3) */
    private String accountNumber3;

    /** Tertiary account holder name (Account 3) */
    private String accountHolderName3;

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
}
