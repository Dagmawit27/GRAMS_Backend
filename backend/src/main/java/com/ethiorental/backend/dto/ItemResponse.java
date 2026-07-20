package com.ethiorental.backend.dto;

public record ItemResponse(
    Long id,
    String name,
    String description,
    Double price
) {}