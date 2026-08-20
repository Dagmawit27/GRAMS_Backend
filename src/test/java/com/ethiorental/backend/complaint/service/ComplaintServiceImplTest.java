package com.ethiorental.backend.complaint.service;

import com.ethiorental.backend.IAM.entity.Citizen;
import com.ethiorental.backend.IAM.entity.CitizenCredential;
import com.ethiorental.backend.IAM.repository.CitizenCredentialRepository;
import com.ethiorental.backend.IAM.repository.EmployeeCredentialRepository;
import com.ethiorental.backend.IAM.repository.GovernmentEmployeeRepository;
import com.ethiorental.backend.complaint.dto.request.SubmitComplaintRequest;
import com.ethiorental.backend.complaint.dto.response.ComplaintResponse;
import com.ethiorental.backend.complaint.entity.Complaint;
import com.ethiorental.backend.complaint.enums.ComplaintCategory;
import com.ethiorental.backend.complaint.enums.ComplaintPriority;
import com.ethiorental.backend.complaint.enums.ComplaintStatus;
import com.ethiorental.backend.complaint.exception.ComplaintNotFoundException;
import com.ethiorental.backend.complaint.exception.ComplaintStatusTransitionException;
import com.ethiorental.backend.complaint.repository.ComplaintAssignmentRepository;
import com.ethiorental.backend.complaint.repository.ComplaintAttachmentRepository;
import com.ethiorental.backend.complaint.repository.ComplaintRepository;
import com.ethiorental.backend.complaint.storage.ComplaintAttachmentStorage;
import com.ethiorental.backend.shared.audit.AuditService;
import com.ethiorental.backend.shared.notification.NotificationEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ComplaintServiceImpl}.
 * <p>
 * These tests inject a mock {@link ComplaintAttachmentStorage} — they are
 * completely decoupled from the local filesystem implementation and will
 * remain valid when the storage impl is swapped for Dev A's
 * {@code DocumentStorageService}.
 */
@ExtendWith(MockitoExtension.class)
class ComplaintServiceImplTest {

    @Mock ComplaintRepository complaintRepository;
    @Mock ComplaintAttachmentRepository attachmentRepository;
    @Mock ComplaintAssignmentRepository assignmentRepository;
    @Mock CitizenCredentialRepository citizenCredentialRepository;
    @Mock EmployeeCredentialRepository employeeCredentialRepository;
    @Mock GovernmentEmployeeRepository governmentEmployeeRepository;
    @Mock ComplaintAttachmentStorage attachmentStorage; // ← storage abstraction, not concrete impl
    @Mock AuditService auditService;
    @Mock NotificationEventPublisher notificationEventPublisher;

    @InjectMocks ComplaintServiceImpl service;

    private Citizen citizen;
    private CitizenCredential citizenCredential;
    private static final String CITIZEN_EMAIL = "citizen1@example.com";

    @BeforeEach
    void setUp() {
        citizen = Citizen.builder()
                .id(UUID.randomUUID())
                .firstName("Abebe")
                .lastName("Bekele")
                .build();

        citizenCredential = CitizenCredential.builder()
                .email(CITIZEN_EMAIL)
                .citizen(citizen)
                .build();

    }

    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("submit()")
    class Submit {

        @Test
        @DisplayName("saves complaint and returns response without attachments")
        void submit_noAttachments_savesPersistsComplaint() {
            SubmitComplaintRequest req = new SubmitComplaintRequest(
                    ComplaintCategory.UNFAIR_RENT_INCREASE,
                    ComplaintPriority.HIGH,
                    "Landlord raised rent illegally",
                    "My landlord raised rent by 50% with no notice.");

            Complaint saved = buildComplaint(req);
            when(citizenCredentialRepository.findByEmail(CITIZEN_EMAIL))
                    .thenReturn(Optional.of(citizenCredential));
            when(complaintRepository.save(any(Complaint.class))).thenReturn(saved);

            ComplaintResponse response = service.submit(req, null, CITIZEN_EMAIL);

            assertThat(response.status()).isEqualTo(ComplaintStatus.SUBMITTED);
            assertThat(response.subject()).isEqualTo(req.subject());
            assertThat(response.attachments()).isEmpty();
            verify(attachmentStorage, never()).store(any(), any()); // no files uploaded
        }

        @Test
        @DisplayName("defaults priority to MEDIUM when not provided")
        void submit_nullPriority_defaultsMedium() {
            SubmitComplaintRequest req = new SubmitComplaintRequest(
                    ComplaintCategory.OTHER, null,
                    "Some complaint", "Some description");

            Complaint saved = Complaint.builder()
                    .id(UUID.randomUUID()).complainant(citizen)
                    .category(req.category()).priority(ComplaintPriority.MEDIUM)
                    .subject(req.subject()).description(req.description())
                    .status(ComplaintStatus.SUBMITTED)
                    .attachments(new ArrayList<>()).assignments(new ArrayList<>())
                    .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                    .build();

            when(citizenCredentialRepository.findByEmail(CITIZEN_EMAIL))
                    .thenReturn(Optional.of(citizenCredential));
            when(complaintRepository.save(any())).thenReturn(saved);

            ComplaintResponse response = service.submit(req, null, CITIZEN_EMAIL);

            assertThat(response.priority()).isEqualTo(ComplaintPriority.MEDIUM);
        }
    }

    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("getById()")
    class GetById {

        @Test
        @DisplayName("throws ComplaintNotFoundException for unknown ID")
        void getById_notFound_throws() {
            UUID unknownId = UUID.randomUUID();
            when(complaintRepository.findById(unknownId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getById(unknownId, CITIZEN_EMAIL))
                    .isInstanceOf(ComplaintNotFoundException.class)
                    .hasMessageContaining(unknownId.toString());
        }

        @Test
        @DisplayName("returns response for existing complaint")
        void getById_found_returnsResponse() {
            Complaint c = buildComplaint(null);
            when(complaintRepository.findById(c.getId())).thenReturn(Optional.of(c));
            when(citizenCredentialRepository.findByEmail(CITIZEN_EMAIL))
                    .thenReturn(Optional.of(citizenCredential));

            ComplaintResponse response = service.getById(c.getId(), CITIZEN_EMAIL);

            assertThat(response.id()).isEqualTo(c.getId());
        }
    }

    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("withdraw()")
    class Withdraw {

        @Test
        @DisplayName("transitions status to WITHDRAWN")
        void withdraw_submittedComplaint_transitions() {
            Complaint c = buildComplaint(null);
            c.setStatus(ComplaintStatus.SUBMITTED);

            when(citizenCredentialRepository.findByEmail(CITIZEN_EMAIL))
                    .thenReturn(Optional.of(citizenCredential));
            when(complaintRepository.findById(c.getId())).thenReturn(Optional.of(c));
            when(complaintRepository.save(c)).thenReturn(c);

            ComplaintResponse response = service.withdraw(c.getId(), CITIZEN_EMAIL);

            assertThat(response.status()).isEqualTo(ComplaintStatus.WITHDRAWN);
        }

        @Test
        @DisplayName("throws when trying to withdraw a RESOLVED complaint")
        void withdraw_resolvedComplaint_throws() {
            Complaint c = buildComplaint(null);
            c.setStatus(ComplaintStatus.RESOLVED);

            when(citizenCredentialRepository.findByEmail(CITIZEN_EMAIL))
                    .thenReturn(Optional.of(citizenCredential));
            when(complaintRepository.findById(c.getId())).thenReturn(Optional.of(c));

            assertThatThrownBy(() -> service.withdraw(c.getId(), CITIZEN_EMAIL))
                    .isInstanceOf(ComplaintStatusTransitionException.class);
        }
    }

    // -----------------------------------------------------------------------
    // Storage abstraction test: proves the service doesn't know or care
    // what implementation of ComplaintAttachmentStorage is active.
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Storage abstraction")
    class StorageAbstraction {

        @Test
        @DisplayName("stores opaque reference — never inspects its content")
        void submit_withAttachment_delegatesToStorageInterface() throws Exception {
            SubmitComplaintRequest req = new SubmitComplaintRequest(
                    ComplaintCategory.ILLEGAL_EVICTION, ComplaintPriority.URGENT,
                    "Eviction notice", "I was evicted without cause.");

            Complaint saved = buildComplaint(req);
            org.springframework.mock.web.MockMultipartFile file =
                    new org.springframework.mock.web.MockMultipartFile(
                            "attachments", "evidence.pdf",
                            "application/pdf", "fake-pdf-content".getBytes());

            when(citizenCredentialRepository.findByEmail(CITIZEN_EMAIL))
                    .thenReturn(Optional.of(citizenCredential));
            when(complaintRepository.save(any())).thenReturn(saved);
            // The storage returns an opaque reference — the service should store it as-is
            when(attachmentStorage.store(eq(saved.getId()), any())).thenReturn("opaque-ref-123");
            when(attachmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.submit(req, List.of(file), CITIZEN_EMAIL);

            // Verify the service called store() through the interface (not any concrete impl)
            verify(attachmentStorage, times(1)).store(eq(saved.getId()), eq(file));
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private Complaint buildComplaint(SubmitComplaintRequest req) {
        return Complaint.builder()
                .id(UUID.randomUUID())
                .complainant(citizen)
                .category(req != null ? req.category() : ComplaintCategory.OTHER)
                .priority(req != null && req.priority() != null ? req.priority() : ComplaintPriority.MEDIUM)
                .subject(req != null ? req.subject() : "Test subject")
                .description(req != null ? req.description() : "Test description")
                .status(ComplaintStatus.SUBMITTED)
                .attachments(new ArrayList<>())
                .assignments(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
