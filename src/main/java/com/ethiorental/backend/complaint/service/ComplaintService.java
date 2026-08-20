package com.ethiorental.backend.complaint.service;

import com.ethiorental.backend.complaint.dto.request.AssignComplaintRequest;
import com.ethiorental.backend.complaint.dto.request.ResolveComplaintRequest;
import com.ethiorental.backend.complaint.dto.request.SubmitComplaintRequest;
import com.ethiorental.backend.complaint.dto.response.ComplaintResponse;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface ComplaintService {

    /**
     * Submit a new complaint (citizen-facing).
     * Attachments are stored via the active {@code ComplaintAttachmentStorage} bean.
     */
    ComplaintResponse submit(SubmitComplaintRequest request,
                             List<MultipartFile> attachments,
                             String username);

    /** Retrieve a single complaint by ID, scoped to the caller. */
    ComplaintResponse getById(UUID complaintId, String username);

    /** All complaints filed by the authenticated citizen. */
    List<ComplaintResponse> getMine(String username);

    /** All complaints — officer/admin view, optionally filtered by status. */
    List<ComplaintResponse> getAll(String statusFilter);

    /**
     * Assign the complaint to an officer (supervisor/admin only).
     * Previous active assignment is revoked automatically.
     */
    ComplaintResponse assign(UUID complaintId,
                             AssignComplaintRequest request,
                             String supervisorUsername);

    /**
     * Record a resolution and transition to RESOLVED.
     */
    ComplaintResponse resolve(UUID complaintId,
                              ResolveComplaintRequest request,
                              String officerUsername);

    /**
     * Withdraw a complaint (citizen-facing — own complaints only).
     */
    ComplaintResponse withdraw(UUID complaintId, String username);

    /**
     * Stream an attachment's binary content for download.
     * Returns the Resource — the controller sets Content-Type and headers.
     */
    Resource downloadAttachment(UUID complaintId, UUID attachmentId, String username);
}
