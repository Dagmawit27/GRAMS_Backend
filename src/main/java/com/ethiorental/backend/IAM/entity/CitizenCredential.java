package com.ethiorental.backend.IAM.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "citizen_credentials")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CitizenCredential {

    @Id
    @UuidGenerator
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "citizen_id", nullable = false, unique = true)
    private Citizen citizen;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    private LocalDateTime lastLogin;
}
