package com.ethiorental.backend.complaint.exception;

public class ComplaintAttachmentNotFoundException extends RuntimeException {

    public ComplaintAttachmentNotFoundException(String storageReference) {
        super("Complaint attachment not found for reference: " + storageReference);
    }

    public ComplaintAttachmentNotFoundException(java.util.UUID attachmentId) {
        super("Complaint attachment not found: " + attachmentId);
    }
}
