package com.ethiorental.backend.report.dto.response;

import lombok.Builder;

import java.time.Instant;

@Builder
public record DashboardMetricsResponse(
        long totalProperties,
        long pendingProperties,
        long verifiedProperties,
        long listedProperties,
        long totalComplaints,
        long openComplaints,
        long resolvedComplaints,
        long totalNotifications,
        long unreadNotifications,
        /** Placeholder — 0 until the agreement module is implemented by Dev B */
        long activeAgreements,
        Instant generatedAt
) {}
