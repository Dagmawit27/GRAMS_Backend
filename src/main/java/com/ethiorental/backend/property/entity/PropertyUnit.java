package com.ethiorental.backend.property.entity;

import com.ethiorental.backend.property.enums.UnitStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "property_units")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PropertyUnit {

    @Id
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @Column(nullable = false, unique = true)
    private String unitCode;

    @Column(nullable = false)
    private String unitName;

    @Column(nullable = false)
    private String unitType;

    @Column(nullable = false)
    private BigDecimal areaSqMeter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UnitStatus status;

    private BigDecimal rentAmount;

    private String tenantName;

    private String floorLevel;

    private String category;

    private String shopNumber;

    private Boolean submeter;

    private Boolean waterSupply;

    private String frontage;

    private String description;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) this.status = UnitStatus.AVAILABLE;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
