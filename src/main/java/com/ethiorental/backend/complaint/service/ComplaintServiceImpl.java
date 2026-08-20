package com.ethiorental.backend.complaint.service;

import com.ethiorental.backend.IAM.entity.Citizen;
import com.ethiorental.backend.IAM.entity.EmployeeRole;
import com.ethiorental.backend.IAM.entity.GovernmentEmployee;
import com.ethiorental.backend.IAM.entity.Role;
import com.ethiorental.backend.IAM.repository.CitizenCredentialRepository;
import com.ethiorental.backend.IAM.repository.EmployeeCredentialRepository;
import com.ethiorental.backend.IAM.repository.EmployeeRoleRepository;
import com.ethiorental.backend.IAM.repository.GovernmentEmployeeRepository;
import com.ethiorental.backend.complaint.dto.request.AssignComplaintRequest;
import com.ethiorental.backend.complaint.dto.request.ResolveComplaintRequest;
import com.ethiorental.backend.complaint.dto.request.SubmitComplaintRequest;
import com.ethiorental.backend.complaint.dto.response.ComplaintResponse;
import com.ethiorental.backend.complaint.entity.Complaint;
import com.ethiorental.backend.complaint.entity.ComplaintAssignment;
import com.ethiorental.backend.complaint.entity.ComplaintAttachment;
import com.ethiorental.backend.complaint.entity.ComplaintResolution;
import com.ethiorental.backend.complaint.enums.ComplaintPriority;
import com.ethiorental.backend.complaint.enums.ComplaintStatus;
import com.ethiorental.backend.complaint.exception.ComplaintAttachmentNotFoundException;
import com.ethiorental.backend.complaint.exception.ComplaintNotFoundException;
import com.ethiorental.backend.complaint.exception.ComplaintStatusTransitionException;
import com.ethiorental.backend.complaint.repository.ComplaintAssignmentRepository;
import com.ethiorental.backend.complaint.repository.ComplaintAttachmentRepository;
import com.ethiorental.backend.complaint.repository.ComplaintRepository;
import com.ethiorental.backend.complaint.storage.ComplaintAttachmentStorage;
import com.ethiorental.backend.shared.audit.AuditAction;
import com.ethiorental.backend.shared.audit.AuditEventRequest;
import com.ethiorental.backend.shared.audit.AuditOutcome;
import com.ethiorental.backend.shared.audit.AuditService;
import com.ethiorental.backend.shared.notification.NotificationChannel;
import com.ethiorental.backend.shared.notification.NotificationEvent;
import com.ethiorental.backend.shared.notification.NotificationEventPublisher;
import com.ethiorental.backend.shared.notification.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Primary implementation of {@link ComplaintService}.
 * <p>
 * Storage is delegated entirely to the active {@link ComplaintAttachmentStorage}
 * bean — currently {@code LocalFileComplaintAttachmentStorage} (dev profile).
 * When Developer A ships {@code DocumentStorageService}, swapping the storage
 * requires <strong>no changes here</strong> beyond the Spring injection point
 * (or a small bridge implementation of {@link ComplaintAttachmentStorage}).
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ComplaintServiceImpl implements ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final ComplaintAttachmentRepository attachmentRepository;
    private final ComplaintAssignmentRepository assignmentRepository;
    private final CitizenCredentialRepository citizenCredentialRepository;
    private final EmployeeCredentialRepository employeeCredentialRepository;
    private final GovernmentEmployeeRepository governmentEmployeeRepository;
    private final ComplaintAttachmentStorage attachmentStorage;
    private final AuditService auditService;
    private final NotificationEventPublisher notificationEventPublisher;
    private final EmployeeRoleRepository employeeRoleRepository;

    // -------------------------------------------------------------------------
    // Citizen-facing operations
    // -------------------------------------------------------------------------

    @Override
    public ComplaintResponse submit(SubmitComplaintRequest request,
                                    List<MultipartFile> attachments,
                                    String username) {
        Citizen complainant = resolveCitizen(username);

        Complaint complaint = Complaint.builder()
                .complainant(complainant)
                .category(request.category())
                .priority(request.priority() != null ? request.priority() : ComplaintPriority.MEDIUM)
                .subject(request.subject())
                .description(request.description())
                .status(ComplaintStatus.SUBMITTED)
                .build();

        complaint = complaintRepository.save(complaint);

        // Store attachments after the complaint is persisted so we have its ID
        if (attachments != null && !attachments.isEmpty()) {
            List<ComplaintAttachment> stored = storeAttachments(complaint, attachments);
            complaint.getAttachments().addAll(stored);
        }

        auditService.log(new AuditEventRequest(
                AuditAction.CREATE,
                "COMPLAINT",
                complaint.getId().toString(),
                null,
                null,
                AuditOutcome.SUCCESS,
                "Complaint submitted by " + username,
                null
        ));

        publishComplaintEvent(
                NotificationType.COMPLAINT_SUBMITTED,
                complainant.getEmail(),
                complaint.getId().toString(),
                "Your complaint has been submitted and is awaiting review.",
                Set.of(NotificationChannel.IN_APP, NotificationChannel.EMAIL, NotificationChannel.SMS)
        );

        log.info("Complaint submitted: id={}, citizen={}", complaint.getId(), username);
        return toResponse(complaint);
    }

    @Override
    @Transactional(readOnly = true)
    public ComplaintResponse getById(UUID complaintId, String username) {
        Complaint complaint = findComplaint(complaintId);
        assertCanViewComplaint(complaint, username);
        return toResponse(complaint);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplaintResponse> getMine(String username) {
        Citizen citizen = resolveCitizen(username);
        return complaintRepository
                .findByComplainant_IdOrderByCreatedAtDesc(citizen.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplaintResponse> getAll(String statusFilter) {
        if (statusFilter != null && !statusFilter.isBlank()) {
            ComplaintStatus status = ComplaintStatus.valueOf(statusFilter.toUpperCase());
            return complaintRepository.findByStatusOrderByCreatedAtAsc(status)
                    .stream().map(this::toResponse).toList();
        }
        return complaintRepository.findAll()
                .stream().map(this::toResponse).toList();
    }

    @Override
    public ComplaintResponse withdraw(UUID complaintId, String username) {
        Citizen citizen = resolveCitizen(username);
        Complaint complaint = findComplaint(complaintId);

        if (!complaint.getComplainant().getId().equals(citizen.getId())) {
            throw new AccessDeniedException("You can only withdraw your own complaints");
        }
        if (complaint.getStatus() == ComplaintStatus.RESOLVED
                || complaint.getStatus() == ComplaintStatus.CLOSED) {
            throw new ComplaintStatusTransitionException(
                    "Cannot withdraw a complaint that is already " + complaint.getStatus());
        }
        ComplaintStatus previousStatus = complaint.getStatus();
        complaint.setStatus(ComplaintStatus.WITHDRAWN);
        Complaint saved = complaintRepository.save(complaint);
        auditService.logStatusChange(
                "COMPLAINT",
                saved.getId().toString(),
                previousStatus.name(),
                saved.getStatus().name(),
                AuditAction.UPDATE,
                "Complaint withdrawn by " + username
        );
        return toResponse(saved);
    }

    // -------------------------------------------------------------------------
    // Officer / supervisor operations
    // -------------------------------------------------------------------------

    @Override
    public ComplaintResponse assign(UUID complaintId,
                                    AssignComplaintRequest request,
                                    String supervisorUsername) {
        Complaint complaint = findComplaint(complaintId);

        if (complaint.getStatus() == ComplaintStatus.RESOLVED
                || complaint.getStatus() == ComplaintStatus.CLOSED
                || complaint.getStatus() == ComplaintStatus.WITHDRAWN) {
            throw new ComplaintStatusTransitionException(
                    String.format("Complaint %s cannot be assigned (current status: %s)",
                            complaintId, complaint.getStatus()));
        }

        GovernmentEmployee supervisor = resolveEmployee(supervisorUsername);
        GovernmentEmployee officer = resolveEmployeeById(request.officerId());
        ComplaintStatus previousStatus = complaint.getStatus();

        // Revoke any existing active assignment
        assignmentRepository.findByComplaint_IdAndRevokedAtIsNull(complaintId)
                .ifPresent(prev -> {
                    prev.setRevokedAt(LocalDateTime.now());
                    assignmentRepository.save(prev);
                });

        ComplaintAssignment assignment = ComplaintAssignment.builder()
                .complaint(complaint)
                .assignedOfficer(officer)
                .assignedBy(supervisor)
                .notes(request.notes())
                .build();
        assignmentRepository.save(assignment);
        complaint.getAssignments().add(assignment);

        complaint.setStatus(ComplaintStatus.UNDER_INVESTIGATION);
        Complaint saved = complaintRepository.save(complaint);

        auditService.logStatusChange(
                "COMPLAINT",
                saved.getId().toString(),
                previousStatus.name(),
                saved.getStatus().name(),
                AuditAction.UPDATE,
                "Complaint assigned to officer " + officer.getId() + " by " + supervisorUsername
        );

        publishComplaintEvent(
                NotificationType.COMPLAINT_ASSIGNED,
                complaint.getComplainant().getEmail(),
                saved.getId().toString(),
                "Your complaint has been assigned to an officer.",
                Set.of(NotificationChannel.IN_APP, NotificationChannel.EMAIL, NotificationChannel.SMS)
        );

        return toResponse(saved);
    }

    @Override
    public ComplaintResponse resolve(UUID complaintId,
                                     ResolveComplaintRequest request,
                                     String officerUsername) {
        Complaint complaint = findComplaint(complaintId);
        GovernmentEmployee officer = resolveEmployee(officerUsername);
        ComplaintAssignment activeAssignment = assignmentRepository.findByComplaint_IdAndRevokedAtIsNull(complaintId)
                .orElseThrow(() -> new ComplaintStatusTransitionException(
                        "Complaint must be assigned before it can be resolved"));

        if (complaint.getStatus() == ComplaintStatus.RESOLVED
                || complaint.getStatus() == ComplaintStatus.CLOSED) {
            throw new ComplaintStatusTransitionException(
                    "Complaint is already " + complaint.getStatus());
        }
        if (complaint.getStatus() != ComplaintStatus.UNDER_INVESTIGATION) {
            throw new ComplaintStatusTransitionException(
                    "Complaint must be under investigation before it can be resolved");
        }
        if (!activeAssignment.getAssignedOfficer().getId().equals(officer.getId())) {
            throw new AccessDeniedException("Only the assigned officer can resolve this complaint");
        }

        ComplaintResolution resolution = ComplaintResolution.builder()
                .complaint(complaint)
                .resolvedBy(officer)
                .resolutionSummary(request.resolutionSummary())
                .outcome(request.outcome())
                .build();

        ComplaintStatus previousStatus = complaint.getStatus();
        complaint.setResolution(resolution);
        complaint.setStatus(ComplaintStatus.RESOLVED);
        Complaint saved = complaintRepository.save(complaint);

        auditService.logStatusChange(
                "COMPLAINT",
                saved.getId().toString(),
                previousStatus.name(),
                saved.getStatus().name(),
                AuditAction.UPDATE,
                "Complaint resolved by officer " + officer.getId()
        );

        publishComplaintEvent(
                NotificationType.COMPLAINT_RESOLVED,
                complaint.getComplainant().getEmail(),
                saved.getId().toString(),
                "Your complaint has been resolved.",
                Set.of(NotificationChannel.IN_APP, NotificationChannel.EMAIL, NotificationChannel.SMS)
        );

        return toResponse(saved);
    }

    // -------------------------------------------------------------------------
    // Attachment download
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public Resource downloadAttachment(UUID complaintId, UUID attachmentId, String username) {
        Complaint complaint = findComplaint(complaintId);
        assertCanViewComplaint(complaint, username);

        // Verify the attachment belongs to this complaint
        ComplaintAttachment attachment = attachmentRepository.findById(attachmentId)
                .filter(a -> a.getComplaint().getId().equals(complaintId))
                .orElseThrow(() -> new ComplaintAttachmentNotFoundException(attachmentId));

        return attachmentStorage.retrieve(attachment.getStorageReference());
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private List<ComplaintAttachment> storeAttachments(Complaint complaint,
                                                        List<MultipartFile> files) {
        List<ComplaintAttachment> result = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;

            String contentHash = computeHash(file);
            String storageRef = attachmentStorage.store(complaint.getId(), file);

            ComplaintAttachment attachment = ComplaintAttachment.builder()
                    .complaint(complaint)
                    .originalFilename(file.getOriginalFilename() != null
                            ? file.getOriginalFilename() : "attachment")
                    .contentType(file.getContentType() != null
                            ? file.getContentType() : "application/octet-stream")
                    .sizeBytes(file.getSize())
                    .contentHash(contentHash)
                    .storageReference(storageRef)
                    .storageVersion(1)
                    .build();

            result.add(attachmentRepository.save(attachment));
        }
        return result;
    }

    private String computeHash(MultipartFile file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(file.getInputStream().readAllBytes());
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to compute attachment hash", e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private Complaint findComplaint(UUID id) {
        return complaintRepository.findById(id)
                .orElseThrow(() -> new ComplaintNotFoundException(id));
    }

    private static final Set<String> SUPERVISOR_ADMIN_ROLES = Set.of(
            "WOREDA_SUPERVISOR", "SUB_CITY_ADMINISTRATOR", "CITY_ADMINISTRATOR", "SYSTEM_ADMINISTRATOR");
    private static final String AUDITOR_ROLE = "AUDITOR";

    private void assertCanViewComplaint(Complaint complaint, String username) {
        if (username == null || username.isBlank()) {
            throw new AccessDeniedException("User is not authenticated");
        }

        // The complainant can always view their own complaint
        Citizen citizen = citizenCredentialRepository.findByEmail(username)
                .map(c -> c.getCitizen())
                .orElse(null);
        if (citizen != null && complaint.getComplainant().getId().equals(citizen.getId())) {
            return;
        }

        // For employees, check specific roles and assignment
        GovernmentEmployee employee = employeeCredentialRepository.findByEmail(username)
                .map(c -> c.getEmployee())
                .orElse(null);
        if (employee == null) {
            throw new AccessDeniedException("You do not have permission to view this complaint");
        }

        // The assigned officer can always view
        UUID employeeId = employee.getId();
        boolean isAssignedOfficer = complaint.getAssignments().stream()
                .filter(a -> a.getRevokedAt() == null)
                .anyMatch(a -> a.getAssignedOfficer().getId().equals(employeeId));
        if (isAssignedOfficer) {
            return;
        }

        // Check employee roles via the employee_roles join table
        List<String> roleNames = employeeRoleRepository.findByEmployee(employee).stream()
                .map(EmployeeRole::getRole)
                .map(Role::getRoleName)
                .toList();

        // Supervisors and admins can view all complaints
        if (roleNames.stream().anyMatch(SUPERVISOR_ADMIN_ROLES::contains)) {
            return;
        }

        // Auditors can view for audit/compliance purposes
        if (roleNames.contains(AUDITOR_ROLE)) {
            return;
        }

        throw new AccessDeniedException("You do not have permission to view this complaint");
    }

    private Citizen resolveCitizen(String username) {
        return citizenCredentialRepository.findByEmail(username)
                .orElseThrow(() -> new IllegalArgumentException("Citizen not found: " + username))
                .getCitizen();
    }

    private GovernmentEmployee resolveEmployee(String username) {
        return employeeCredentialRepository.findByEmail(username)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + username))
                .getEmployee();
    }

    private GovernmentEmployee resolveEmployeeById(UUID officerId) {
        return governmentEmployeeRepository.findById(officerId)
                .orElseThrow(() -> new IllegalArgumentException("Officer not found: " + officerId));
    }

    private void publishComplaintEvent(NotificationType type,
                                       String recipientUserId,
                                       String complaintId,
                                       String message,
                                       Set<NotificationChannel> preferredChannels) {
        notificationEventPublisher.publish(new NotificationEvent(
                type,
                recipientUserId,
                "COMPLAINT",
                complaintId,
                message,
                preferredChannels
        ));
    }

    // -------------------------------------------------------------------------
    // Mapping
    // -------------------------------------------------------------------------

    private ComplaintResponse toResponse(Complaint c) {
        // Build attachment summaries — storageReference is NOT exposed in response
        List<ComplaintResponse.AttachmentSummary> attachmentSummaries = c.getAttachments()
                .stream()
                .map(a -> new ComplaintResponse.AttachmentSummary(
                        a.getId(),
                        a.getOriginalFilename(),
                        a.getContentType(),
                        a.getSizeBytes(),
                        "/api/v1/complaints/" + c.getId() + "/attachments/" + a.getId(),
                        a.getUploadedAt()))
                .toList();

        // Active assignment
        ComplaintResponse.AssignmentSummary assignmentSummary = null;
        if (!c.getAssignments().isEmpty()) {
            ComplaintAssignment active = c.getAssignments().stream()
                    .filter(a -> a.getRevokedAt() == null)
                    .findFirst().orElse(null);
            if (active != null) {
                GovernmentEmployee officer = active.getAssignedOfficer();
                assignmentSummary = new ComplaintResponse.AssignmentSummary(
                        active.getId(),
                        officer.getId(),
                        officer.getFirstName() + " " + officer.getLastName(),
                        active.getNotes(),
                        active.getAssignedAt());
            }
        }

        // Resolution
        ComplaintResponse.ResolutionSummary resolutionSummary = null;
        if (c.getResolution() != null) {
            ComplaintResolution r = c.getResolution();
            resolutionSummary = new ComplaintResponse.ResolutionSummary(
                    r.getId(),
                    r.getResolutionSummary(),
                    r.getOutcome(),
                    r.getResolvedBy().getFirstName() + " " + r.getResolvedBy().getLastName(),
                    r.getResolvedAt());
        }

        Citizen complainant = c.getComplainant();
        return new ComplaintResponse(
                c.getId(),
                complainant.getId(),
                complainant.getFirstName() + " " + complainant.getLastName(),
                c.getCategory(),
                c.getPriority(),
                c.getSubject(),
                c.getDescription(),
                c.getStatus(),
                attachmentSummaries,
                assignmentSummary,
                resolutionSummary,
                c.getCreatedAt(),
                c.getUpdatedAt());
    }
}
