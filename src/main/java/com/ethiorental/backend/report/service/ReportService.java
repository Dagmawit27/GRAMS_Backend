package com.ethiorental.backend.report.service;

import com.ethiorental.backend.complaint.enums.ComplaintStatus;
import com.ethiorental.backend.complaint.repository.ComplaintRepository;
import com.ethiorental.backend.notification.repository.NotificationRepository;
import com.ethiorental.backend.property.enums.PropertyStatus;
import com.ethiorental.backend.property.repository.PropertyRepository;
import com.ethiorental.backend.report.dto.response.ComplaintReportResponse;
import com.ethiorental.backend.report.dto.response.DashboardMetricsResponse;
import com.ethiorental.backend.report.dto.response.PropertyReportResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Read-only reporting service that aggregates data across modules for
 * admin dashboards and compliance reporting (SRS §5.9).
 *
 * <p>All methods are {@code @Transactional(readOnly = true)} to avoid
 * dirty-checking overhead and to enable read replicas in future.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private final PropertyRepository propertyRepository;
    private final ComplaintRepository complaintRepository;
    private final NotificationRepository notificationRepository;

    // ──────────────────────────────────────────────────────────────────────────
    // Dashboard
    // ──────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public DashboardMetricsResponse getDashboardMetrics() {
        long totalProperties     = propertyRepository.count();
        long pendingProperties   = propertyRepository.countByStatus(PropertyStatus.PENDING);
        long verifiedProperties  = propertyRepository.countByStatus(PropertyStatus.VERIFIED);
        long listedProperties    = propertyRepository.countByStatus(PropertyStatus.LISTED);

        long totalComplaints     = complaintRepository.count();
        long openComplaints      = complaintRepository.countByStatus(ComplaintStatus.SUBMITTED)
                                 + complaintRepository.countByStatus(ComplaintStatus.UNDER_INVESTIGATION);
        long resolvedComplaints  = complaintRepository.countByStatus(ComplaintStatus.RESOLVED);

        // notificationRepository counts across all users — useful system-wide metric
        long totalNotifications  = notificationRepository.count();
        // Note: unread-count across ALL users (not per-user) as a system metric
        long unreadNotifications = notificationRepository.countByReadFalse();

        return DashboardMetricsResponse.builder()
                .totalProperties(totalProperties)
                .pendingProperties(pendingProperties)
                .verifiedProperties(verifiedProperties)
                .listedProperties(listedProperties)
                .totalComplaints(totalComplaints)
                .openComplaints(openComplaints)
                .resolvedComplaints(resolvedComplaints)
                .totalNotifications(totalNotifications)
                .unreadNotifications(unreadNotifications)
                .activeAgreements(0L) // placeholder until Dev B ships the agreement module
                .generatedAt(Instant.now())
                .build();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Property report
    // ──────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PropertyReportResponse getPropertyReport() {
        long total = propertyRepository.count();

        Map<String, Long> byStatus = toStringLongMap(propertyRepository.countGroupedByStatus());
        Map<String, Long> byType   = toStringLongMap(propertyRepository.countGroupedByType());

        return PropertyReportResponse.builder()
                .totalProperties(total)
                .byStatus(byStatus)
                .byType(byType)
                .reportGeneratedAt(Instant.now())
                .build();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Complaint report
    // ──────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ComplaintReportResponse getComplaintReport() {
        long total = complaintRepository.count();

        Map<String, Long> byStatus   = toStringLongMap(complaintRepository.countGroupedByStatus());
        Map<String, Long> byCategory = toStringLongMap(complaintRepository.countGroupedByCategory());
        Map<String, Long> byPriority = toStringLongMap(complaintRepository.countGroupedByPriority());

        Double avgDays = complaintRepository.averageResolutionDays();
        double averageResolutionDays = (avgDays != null) ? avgDays : 0.0;

        return ComplaintReportResponse.builder()
                .totalComplaints(total)
                .byStatus(byStatus)
                .byCategory(byCategory)
                .byPriority(byPriority)
                .averageResolutionDays(averageResolutionDays)
                .reportGeneratedAt(Instant.now())
                .build();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Converts a list of [key, count] Object[] rows (from JPQL GROUP BY queries)
     * into a sorted LinkedHashMap<String, Long>.
     */
    private Map<String, Long> toStringLongMap(List<Object[]> rows) {
        Map<String, Long> result = new LinkedHashMap<>();
        for (Object[] row : rows) {
            String key   = (row[0] != null) ? row[0].toString() : "UNKNOWN";
            Long   count = ((Number) row[1]).longValue();
            result.put(key, count);
        }
        return result;
    }
}
