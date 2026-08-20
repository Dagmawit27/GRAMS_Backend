package com.ethiorental.backend.complaint.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * Abstraction for complaint attachment binary storage.
 * <p>
 * <strong>Design note:</strong> This interface is a <em>temporary bridge</em>
 * owned by Developer C (Complaint module).  It will be replaced once
 * Developer A ships the shared {@code DocumentStorageService} / MinIO adapter
 * (SRS §6.6).  At that point, the active implementation of this interface
 * (or the interface itself) will be deleted and the complaint service will
 * call {@code DocumentStorageService} directly — with <strong>zero changes</strong>
 * to {@link com.ethiorental.backend.complaint.entity.ComplaintAttachment},
 * any controller, or any test, because the entity stores only an opaque
 * {@code storageReference} string.
 * <p>
 * Implementations must be thread-safe.
 */
public interface ComplaintAttachmentStorage {

    /**
     * Persist {@code file} and return an opaque storage reference that can be
     * used to retrieve or delete the file later.
     *
     * @param complaintId  the complaint this attachment belongs to (used by
     *                     implementations to namespace/organise storage)
     * @param file         the multipart file to store
     * @return             an opaque, non-null reference string
     */
    String store(UUID complaintId, MultipartFile file);

    /**
     * Return the stored file as a Spring {@link Resource}.
     *
     * @param storageReference the opaque reference returned by {@link #store}
     * @return                 a readable, non-null resource
     * @throws java.util.NoSuchElementException if the reference cannot be resolved
     */
    Resource retrieve(String storageReference);

    /**
     * Remove the stored file.  A best-effort operation — implementations should
     * log failures but must not propagate exceptions that would roll back a
     * database transaction that has already committed.
     *
     * @param storageReference the opaque reference returned by {@link #store}
     */
    void delete(String storageReference);
}
