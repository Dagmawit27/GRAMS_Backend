package com.ethiorental.backend.location.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Reference table of all official sub-cities and their woredas in Addis Ababa.
 * Seeded at startup, used for validation of property registrations and employee assignments.
 */
@Entity
@Table(name = "sub_city_woredas",
       uniqueConstraints = @UniqueConstraint(columnNames = {"sub_city", "woreda"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SubCityWoreda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sub_city", nullable = false)
    private String subCity;

    @Column(nullable = false)
    private String woreda;
}
