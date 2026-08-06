package com.ethiorental.backend.IAM.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Entity
@Table(name = "offices")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Office {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(nullable = false)
    private String officeName;

    @Column(nullable = false)
    private String officeType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_office_id")
    private Office parentOffice;
}
