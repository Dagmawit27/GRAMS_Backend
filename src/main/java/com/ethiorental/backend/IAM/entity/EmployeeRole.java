package com.ethiorental.backend.IAM.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Entity
@Table(name = "employee_roles",
       uniqueConstraints = @UniqueConstraint(columnNames = {"employee_id", "role_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EmployeeRole {

    @Id
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private GovernmentEmployee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;
}
