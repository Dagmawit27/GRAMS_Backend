package com.ethiorental.backend.complaint.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import com.ethiorental.backend.complaint.exception.AttachmentValidationException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;

/**
 * ⚠️  TEMPORARY — LOCAL FILESYSTEM STORAGE — DEV PROFILE ONLY ⚠️
 * <p>
 * This class is a placeholder implementation of {@link ComplaintAttachmentStorage}
 * that saves files to the local filesystem. It exists solely to unblock
 * Developer C's complaint module while Developer A's MinIO adapter
 * ({@code DocumentStorageService}) is still in progress (Sprint 0 / Sprint 1).
 * <p>
 * <strong>TODO (swap when Dev A ships):</strong>
 * <ol>
 *   <li>Delete this class entirely.</li>
 *   <li>Delete or keep {@link ComplaintAttachmentStorage} depending on whether
 *       {@code DocumentStorageService} can be called directly from
 *       {@code ComplaintService}.</li>
 *   <li>In {@code ComplaintService}, inject {@code DocumentStorageService}
 *       (Dev A's bean) instead of {@code ComplaintAttachmentStorage}.</li>
 *   <li>Update the Flyway migration if the {@code storageVersion} column
 *       semantics change.</li>
 * </ol>
 * <p>
 * This bean is active in any Spring profile except {@code prod} via
 * {@code @Profile("!prod")} — it <strong>cannot</strong> start in the
 * {@code prod} profile, providing a hard compile-time guard against
 * accidentally shipping this to production.
 *
 * @see ComplaintAttachmentStorage
 */
@Slf4j
@Component
@Profile("!prod")
public class LocalFileComplaintAttachmentStorage implements ComplaintAttachmentStorage {

    private final Path rootDir;
    private final long maxFileSizeBytes;

    /** Allowed content types for complaint attachments. */
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp",
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    public LocalFileComplaintAttachmentStorage(
            @Value("${app.complaint.storage.local-dir:${user.home}/.grams-dev/complaint-attachments}")
            String localDir,
            @Value("${complaint.attachment.max-size-bytes:10485760}")
            long maxFileSizeBytes) {
        this.rootDir = Paths.get(localDir).toAbsolutePath().normalize();
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    @PostConstruct
    void init() {
        try {
            Files.createDirectories(rootDir);
            log.warn("⚠️  [DEV ONLY] LocalFileComplaintAttachmentStorage active — " +
                     "storing complaint attachments under: {}", rootDir);
        } catch (IOException e) {
            throw new UncheckedIOException(
                "Failed to initialise local complaint attachment storage directory: " + rootDir, e);
        }
    }

    /**
     * Stores the file under {@code <rootDir>/<complaintId>/<uuid>_<originalFilename>}.
     * <p>
     * The returned opaque reference is the <em>relative path</em> from rootDir,
     * formatted as {@code "<complaintId>/<uuid>_<originalFilename>"}.
     * This deliberately does <strong>not</strong> expose absolute paths (SRS §10.8).
     */
    @Override
    public String store(UUID complaintId, MultipartFile file) {
        validateFile(file);
        try {
            Path complaintDir = rootDir.resolve(complaintId.toString());
            Files.createDirectories(complaintDir);

            String safeFilename = UUID.randomUUID() + "_" + sanitise(file.getOriginalFilename());
            Path destination = complaintDir.resolve(safeFilename);
            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);

            // Return relative opaque reference — never the absolute path
            String ref = complaintId + "/" + safeFilename;
            log.debug("Stored complaint attachment: ref={}", ref);
            return ref;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store complaint attachment", e);
        }
    }

    @Override
    public Resource retrieve(String storageReference) {
        try {
            Path filePath = rootDir.resolve(storageReference).normalize();
            // Security check: prevent path traversal
            if (!filePath.startsWith(rootDir)) {
                throw new IllegalArgumentException("Invalid storage reference: " + storageReference);
            }
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new NoSuchElementException(
                    "Attachment not found or not readable: " + storageReference);
            }
            return resource;
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Malformed storage reference: " + storageReference, e);
        }
    }

    @Override
    public void delete(String storageReference) {
        try {
            Path filePath = rootDir.resolve(storageReference).normalize();
            if (!filePath.startsWith(rootDir)) {
                log.warn("Refused to delete outside root — ref: {}", storageReference);
                return;
            }
            boolean deleted = Files.deleteIfExists(filePath);
            if (deleted) {
                log.debug("Deleted complaint attachment: ref={}", storageReference);
            } else {
                log.warn("Attachment not found during delete (already removed?): ref={}", storageReference);
            }
        } catch (IOException e) {
            // Best-effort — log but don't propagate (entity may already be deleted)
            log.error("Failed to delete complaint attachment: ref={}", storageReference, e);
        }
    }

    // ---------- validation ----------

    private void validateFile(MultipartFile file) {
        if (file.getSize() > maxFileSizeBytes) {
            throw new AttachmentValidationException(
                    String.format("File size %d bytes exceeds maximum allowed size of %d bytes (%d MB)",
                            file.getSize(), maxFileSizeBytes, maxFileSizeBytes / (1024 * 1024)));
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new AttachmentValidationException(
                    String.format("Content type '%s' is not allowed. Allowed types: %s",
                            contentType, ALLOWED_CONTENT_TYPES));
        }
    }

    // ---------- helpers ----------

    private static String sanitise(String filename) {
        if (filename == null || filename.isBlank()) return "attachment";
        // Keep only alphanumeric, dots, dashes, underscores
        return filename.replaceAll("[^a-zA-Z0-9.\\-_]", "_");
    }
}
