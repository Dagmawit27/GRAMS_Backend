package com.ethiorental.backend.complaint.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Attachment metadata for a complaint.
 * <p>
 * SRS §6.6 — Attachment storage is MinIO-backed (Developer A's scope).
 * SRS §10.8 — Table stores document metadata, content-hash, storage reference,
 *             and version — never a raw filesystem path.
 * <p>
 * The {@code storageReference} is an opaque string whose format is defined
 * by the {@link com.ethiorental.backend.complaint.storage.ComplaintAttachmentStorage}
 * implementation in use. Swapping from the local-filesystem placeholder to
 * Developer A's {@code DocumentStorageService} / MinIO adapter requires only
 * replacing the storage bean — no changes to this entity, the controller, or tests.
 */
@Entity
@Table(name = "complaint_attachments")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ComplaintAttachment {

    @Id
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "complaint_id", nullable = false)
    private Complaint complaint;

    /** Original filename provided by the uploader (display only). */
    @Column(nullable = false)
    private String originalFilename;

    /** MIME type, e.g. "image/jpeg", "application/pdf". */
    @Column(nullable = false)
    private String contentType;

    /** File size in bytes. */
    @Column(nullable = false)
    private Long sizeBytes;

    /**
     * SHA-256 hex digest of the file content.
     * Required by SRS §10.8 for integrity verification.
     */
    @Column(nullable = false, length = 64)
    private String contentHash;

    /**
     * Opaque reference used by the active
     * {@link com.ethiorental.backend.complaint.storage.ComplaintAttachmentStorage}
     * implementation to locate the file.
     * Never a raw filesystem path — always an implementation-defined key.
     */
    @Column(nullable = false)
    private String storageReference;

    /** Storage schema version — allows migrations between storage implementations. */
    @Column(nullable = false)
    @Builder.Default
    private Integer storageVersion = 1;

    @Column(nullable = false, updatable = false)
    private LocalDateTime uploadedAt;

    @PrePersist
    protected void onCreate() {
        this.uploadedAt = LocalDateTime.now();
    }
}
