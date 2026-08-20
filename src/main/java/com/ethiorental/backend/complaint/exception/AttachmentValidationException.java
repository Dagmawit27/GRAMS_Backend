package com.ethiorental.backend.complaint.exception;

/**
 * Thrown when a complaint attachment fails basic validation
 * (file size limit, content-type allowlist) during upload.
 */
public class AttachmentValidationException extends RuntimeException {

    public AttachmentValidationException(String message) {
        super(message);
    }

    public AttachmentValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
