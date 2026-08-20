package com.ethiorental.backend.complaint.dto.response;

import com.ethiorental.backend.complaint.enums.ComplaintCategory;
import com.ethiorental.backend.complaint.enums.ComplaintPriority;
import com.ethiorental.backend.complaint.enums.ComplaintStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Full complaint response including attachments, assignment, and resolution.
 */
public record ComplaintResponse(
    UUID id,
    UUID complainantId,
    String complainantName,
    ComplaintCategory category,
    ComplaintPriority priority,
    String subject,
    String description,
    ComplaintStatus status,
    List<AttachmentSummary> attachments,
    AssignmentSummary currentAssignment,
    ResolutionSummary resolution,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {

    public record AttachmentSummary(
        UUID id,
        String originalFilename,
        String contentType,
        Long sizeBytes,
        /** Download URL — relative path to the download endpoint */
        String downloadUrl,
        LocalDateTime uploadedAt
    ) {}

    public record AssignmentSummary(
        UUID assignmentId,
        UUID officerId,
        String officerName,
        String notes,
        LocalDateTime assignedAt
    ) {}

    public record ResolutionSummary(
        UUID resolutionId,
        String resolutionSummary,
        String outcome,
        String resolvedByName,
        LocalDateTime resolvedAt
    ) {}
}
