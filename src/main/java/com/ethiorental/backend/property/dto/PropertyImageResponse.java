package com.ethiorental.backend.property.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record PropertyImageResponse(
        UUID id,
        String imageUrl,
        boolean isCover,
        LocalDateTime uploadedAt
) {}
