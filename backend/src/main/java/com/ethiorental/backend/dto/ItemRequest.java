package com.ethiorental.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ItemRequest(
    @NotBlank(message = "Name is required") 
    String name,
    
    String description,
    
    @NotNull(message = "Price is required") 
    @Positive(message = "Price must be positive") 
    Double price
) {}