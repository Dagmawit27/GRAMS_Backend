package com.ethiorental.backend.property.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "ownership_documents")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OwnershipDocument {

    @Id
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @Column(nullable = false, unique = true)
    private String documentNumber;

    @Column(nullable = false)
    private String documentType;

    @Column(nullable = false)
    private String filePath;

    private LocalDate issueDate;

    private LocalDate expiryDate;
}
