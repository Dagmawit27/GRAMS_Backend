package com.ethiorental.backend.IAM.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "employee_credentials")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EmployeeCredential {

    @Id
    @UuidGenerator
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false, unique = true)
    private GovernmentEmployee employee;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String passwordHash;

    private LocalDateTime lastLogin;
}
