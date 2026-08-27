package com.ethiorental.backend.location.repository;

import com.ethiorental.backend.location.entity.SubCityWoreda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubCityWoredaRepository extends JpaRepository<SubCityWoreda, Long> {
    List<SubCityWoreda> findBySubCityIgnoreCaseOrderByWoreda(String subCity);
    boolean existsBySubCityIgnoreCaseAndWoreda(String subCity, String woreda);
    List<SubCityWoreda> findAllByOrderBySubCityAscWoredaAsc();
}
