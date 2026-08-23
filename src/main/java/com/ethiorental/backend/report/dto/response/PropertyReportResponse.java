package com.ethiorental.backend.report.dto.response;

import lombok.Builder;

import java.time.Instant;
import java.util.Map;

@Builder
public record PropertyReportResponse(
        long totalProperties,
        Map<String, Long> byStatus,
        Map<String, Long> byType,
        Instant reportGeneratedAt
) {}
