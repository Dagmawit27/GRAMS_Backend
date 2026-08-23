package com.ethiorental.backend.report.controller;

import com.ethiorental.backend.report.dto.response.ComplaintReportResponse;
import com.ethiorental.backend.report.dto.response.DashboardMetricsResponse;
import com.ethiorental.backend.report.dto.response.PropertyReportResponse;
import com.ethiorental.backend.report.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for reporting and analytics (SRS §5.9).
 *
 * <p>Route summary:
 * <pre>
 *   GET /api/v1/reports/dashboard     — overall system metrics (admin/auditor)
 *   GET /api/v1/reports/properties    — property breakdown by status & type
 *   GET /api/v1/reports/complaints    — complaint breakdown + avg resolution time
 * </pre>
 */
@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'CITY_ADMINISTRATOR', 'AUDITOR')")
public class ReportController {

    private final ReportService reportService;

    /** System-wide dashboard metrics for the admin analytics view. */
    @GetMapping("/dashboard")
    public ResponseEntity<DashboardMetricsResponse> getDashboardMetrics() {
        return ResponseEntity.ok(reportService.getDashboardMetrics());
    }

    /** Property report: total, breakdown by status and property type. */
    @GetMapping("/properties")
    public ResponseEntity<PropertyReportResponse> getPropertyReport() {
        return ResponseEntity.ok(reportService.getPropertyReport());
    }

    /** Complaint report: total, breakdown by status/category/priority + avg resolution time. */
    @GetMapping("/complaints")
    public ResponseEntity<ComplaintReportResponse> getComplaintReport() {
        return ResponseEntity.ok(reportService.getComplaintReport());
    }
}
