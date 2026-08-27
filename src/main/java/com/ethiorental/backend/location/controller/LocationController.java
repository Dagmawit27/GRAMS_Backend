package com.ethiorental.backend.location.controller;

import com.ethiorental.backend.location.dto.SubCityDto;
import com.ethiorental.backend.location.entity.SubCityWoreda;
import com.ethiorental.backend.location.repository.SubCityWoredaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/locations")
@RequiredArgsConstructor
public class LocationController {

    private final SubCityWoredaRepository repo;

    /**
     * Returns all Addis Ababa sub-cities, each with their woredas.
     * Public endpoint — used by the property registration form and admin employee form.
     */
    @GetMapping("/sub-cities")
    public ResponseEntity<List<SubCityDto>> getAllSubCities() {
        List<SubCityWoreda> all = repo.findAllByOrderBySubCityAscWoredaAsc();
        // Group by subCity → sorted list of woredas
        Map<String, List<String>> grouped = new LinkedHashMap<>();
        for (SubCityWoreda row : all) {
            grouped.computeIfAbsent(row.getSubCity(), k -> new ArrayList<>()).add(row.getWoreda());
        }
        List<SubCityDto> result = grouped.entrySet().stream()
                .map(e -> new SubCityDto(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    /**
     * Returns woredas for a specific sub-city.
     */
    @GetMapping("/sub-cities/{subCity}/woredas")
    public ResponseEntity<List<String>> getWoredas(@PathVariable String subCity) {
        List<String> woredas = repo.findBySubCityIgnoreCaseOrderByWoreda(subCity)
                .stream().map(SubCityWoreda::getWoreda).toList();
        return ResponseEntity.ok(woredas);
    }

    /**
     * Validates that a sub-city + woreda combination exists.
     */
    @GetMapping("/validate")
    public ResponseEntity<Map<String, Boolean>> validate(
            @RequestParam String subCity,
            @RequestParam String woreda) {
        boolean valid = repo.existsBySubCityIgnoreCaseAndWoreda(subCity, woreda);
        return ResponseEntity.ok(Map.of("valid", valid));
    }
}
