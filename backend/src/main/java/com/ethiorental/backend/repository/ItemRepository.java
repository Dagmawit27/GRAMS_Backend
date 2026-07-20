package com.ethiorental.backend.repository;

import com.ethiorental.backend.Entity.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemRepository extends JpaRepository<Item, Long> {}