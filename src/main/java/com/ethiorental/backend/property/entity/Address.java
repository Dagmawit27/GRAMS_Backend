package com.ethiorental.backend.property.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "addresses")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Address {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String subCity;

    @Column(nullable = false)
    private String woreda;

    private String kebele;

    private String street;

    private String houseNumber;

    private BigDecimal latitude;

    private BigDecimal longitude;
}
