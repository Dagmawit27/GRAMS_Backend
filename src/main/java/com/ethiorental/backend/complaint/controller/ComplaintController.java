package com.ethiorental.backend.complaint.controller;

import com.ethiorental.backend.complaint.dto.request.AssignComplaintRequest;
import com.ethiorental.backend.complaint.dto.request.ResolveComplaintRequest;
import com.ethiorental.backend.complaint.dto.request.SubmitComplaintRequest;
import com.ethiorental.backend.complaint.dto.response.ComplaintResponse;
import com.ethiorental.backend.complaint.service.ComplaintService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for the Complaint Management module (SRS §5.8).
 * <p>
 * Route summary:
 * <pre>
 *   POST   /api/v1/complaints                              — citizen submits complaint
 *   GET    /api/v1/complaints                              — officer/admin: all complaints
 *   GET    /api/v1/complaints/my                           — citizen: own complaints
 *   GET    /api/v1/complaints/{id}                         — get single complaint
 *   PUT    /api/v1/complaints/{id}/assign                  — assign to officer
 *   PUT    /api/v1/complaints/{id}/resolve                 — record resolution
 *   PUT    /api/v1/complaints/{id}/withdraw                — citizen withdraws
 *   GET    /api/v1/complaints/{id}/attachments/{aid}       — download attachment
 * </pre>
 */
@RestController
@RequestMapping("/api/v1/complaints")
@RequiredArgsConstructor
public class ComplaintController {

    private final ComplaintService complaintService;

    // -------------------------------------------------------------------------
    // Citizen operations
    // -------------------------------------------------------------------------

    /**
     * Submit a new complaint with optional attachments.
     * Accepts multipart/form-data: JSON part "complaint" + optional "attachments" files.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('CITIZEN')")
    public ResponseEntity<ComplaintResponse> submit(
            @RequestPart("complaint") @Valid SubmitComplaintRequest request,
            @RequestPart(value = "attachments", required = false) List<MultipartFile> attachments,
            @AuthenticationPrincipal UserDetails userDetails) {

        ComplaintResponse response = complaintService.submit(request, attachments, userDetails.getUsername());
        return ResponseEntity.status(201).body(response);
    }

    /**
     * Get all complaints filed by the authenticated citizen.
     */
    @GetMapping("/my")
    @PreAuthorize("hasRole('CITIZEN')")
    public ResponseEntity<List<ComplaintResponse>> getMine(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(complaintService.getMine(userDetails.getUsername()));
    }

    /**
     * Withdraw a complaint (citizen-facing — own complaints only).
     */
    @PutMapping("/{id}/withdraw")
    @PreAuthorize("hasRole('CITIZEN')")
    public ResponseEntity<ComplaintResponse> withdraw(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(complaintService.withdraw(id, userDetails.getUsername()));
    }

    // -------------------------------------------------------------------------
    // Shared — citizen can see their own, officers see all (enforced in service)
    // -------------------------------------------------------------------------

    /**
     * Get a single complaint by ID.
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ComplaintResponse> getById(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(complaintService.getById(id, userDetails.getUsername()));
    }

    /**
     * Download an attachment binary.
     * The storageReference is never exposed — only attachment IDs are used.
     */
    @GetMapping("/{id}/attachments/{attachmentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Resource> downloadAttachment(
            @PathVariable UUID id,
            @PathVariable UUID attachmentId,
            @AuthenticationPrincipal UserDetails userDetails) {

        Resource resource = complaintService.downloadAttachment(id, attachmentId, userDetails.getUsername());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(resource.getFilename() != null
                                        ? resource.getFilename() : "attachment")
                                .build().toString())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    // -------------------------------------------------------------------------
    // Officer / admin operations
    // -------------------------------------------------------------------------

    /**
     * List all complaints — optionally filter by status query param.
     * e.g. GET /api/v1/complaints?status=SUBMITTED
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('WOREDA_OFFICER','WOREDA_SUPERVISOR','SUB_CITY_ADMINISTRATOR'," +
                             "'CITY_ADMINISTRATOR','SYSTEM_ADMINISTRATOR','AUDITOR')")
    public ResponseEntity<List<ComplaintResponse>> getAll(
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(complaintService.getAll(status));
    }

    /**
     * Assign complaint to an officer.
     */
    @PutMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('WOREDA_SUPERVISOR','SUB_CITY_ADMINISTRATOR'," +
                             "'CITY_ADMINISTRATOR','SYSTEM_ADMINISTRATOR')")
    public ResponseEntity<ComplaintResponse> assign(
            @PathVariable UUID id,
            @RequestBody @Valid AssignComplaintRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(complaintService.assign(id, request, userDetails.getUsername()));
    }

    /**
     * Record the resolution of a complaint.
     */
    @PutMapping("/{id}/resolve")
    @PreAuthorize("hasAnyRole('WOREDA_OFFICER','WOREDA_SUPERVISOR','SUB_CITY_ADMINISTRATOR'," +
                             "'CITY_ADMINISTRATOR','SYSTEM_ADMINISTRATOR')")
    public ResponseEntity<ComplaintResponse> resolve(
            @PathVariable UUID id,
            @RequestBody @Valid ResolveComplaintRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(complaintService.resolve(id, request, userDetails.getUsername()));
    }
}
