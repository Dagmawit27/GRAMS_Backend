package com.ethiorental.backend.property.repository;

import com.ethiorental.backend.property.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AddressRepository extends JpaRepository<Address, UUID> {
}
