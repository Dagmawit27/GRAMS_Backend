package com.ethiorental.backend.complaint.dto.request;

import com.ethiorental.backend.complaint.enums.ComplaintCategory;
import com.ethiorental.backend.complaint.enums.ComplaintPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request body for submitting a new complaint.
 * Attachments are passed as separate multipart file parts — see controller.
 */
public record SubmitComplaintRequest(

    @NotNull(message = "Category is required")
    ComplaintCategory category,

    ComplaintPriority priority,   // optional — defaults to MEDIUM in entity

    @NotBlank(message = "Subject is required")
    @Size(max = 255, message = "Subject must be 255 characters or fewer")
    String subject,

    @NotBlank(message = "Description is required")
    @Size(max = 5000, message = "Description must be 5000 characters or fewer")
    String description
) {}
