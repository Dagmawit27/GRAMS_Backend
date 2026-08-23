package com.ethiorental.backend.report.dto.response;

import lombok.Builder;

import java.time.Instant;
import java.util.Map;

@Builder
public record ComplaintReportResponse(
        long totalComplaints,
        Map<String, Long> byStatus,
        Map<String, Long> byCategory,
        Map<String, Long> byPriority,
        double averageResolutionDays,
        Instant reportGeneratedAt
) {}
