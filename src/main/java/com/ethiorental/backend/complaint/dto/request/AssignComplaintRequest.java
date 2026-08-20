package com.ethiorental.backend.complaint.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Request body for assigning a complaint to an officer.
 */
public record AssignComplaintRequest(

    @NotNull(message = "Officer ID is required")
    UUID officerId,

    @NotBlank(message = "Notes are required when assigning")
    String notes
) {}
