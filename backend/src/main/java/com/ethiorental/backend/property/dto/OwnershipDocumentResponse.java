package com.ethiorental.backend.property.dto;

import java.time.LocalDate;
import java.util.UUID;

public record OwnershipDocumentResponse(
        UUID id,
        String documentNumber,
        String documentType,
        String filePath,
        LocalDate issueDate,
        LocalDate expiryDate
) {}
