package com.ethiorental.backend.complaint.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for resolving or closing a complaint.
 */
public record ResolveComplaintRequest(

    @NotBlank(message = "Resolution summary is required")
    String resolutionSummary,

    @NotBlank(message = "Outcome is required")
    String outcome
) {}
