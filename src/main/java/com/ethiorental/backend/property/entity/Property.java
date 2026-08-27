package com.ethiorental.backend.property.entity;

import com.ethiorental.backend.IAM.entity.Citizen;
import com.ethiorental.backend.property.enums.PropertyStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "properties")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Property {

    @Id
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "landlord_id", nullable = false)
    @org.hibernate.annotations.NotFound(action = org.hibernate.annotations.NotFoundAction.IGNORE)
    private Citizen landlord;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "address_id", nullable = false)
    private Address address;

    @Column(nullable = false, unique = true)
    private String propertyCode;

    /** Human-readable listing title (e.g. "Bole Atlas Luxury Villa") */
    private String title;

    @Column(nullable = false)
    private String propertyType;

    private String houseNumber;

    private String floorNumber;

    private Integer bedroomCount;

    private Integer bathroomCount;

    private BigDecimal areaSqMeter;

    @Column(nullable = false)
    private BigDecimal monthlyRent;

    private String furnishingStatus;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String ownershipType;

    private String specificLandmark;

    private String cadastralParcelId;

    @Column(unique = true)
    private String titleDeedNumber;

    private Integer securityDepositMonths;

    private String minLeasePeriod;

    private String availableFrom;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PropertyStatus status;

    @OneToMany(mappedBy = "property", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OwnershipDocument> ownershipDocuments;

    @OneToMany(mappedBy = "property", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PropertyImage> images;

    @OneToMany(mappedBy = "property", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PropertyUnit> units;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) this.status = PropertyStatus.PENDING;
    }
}
