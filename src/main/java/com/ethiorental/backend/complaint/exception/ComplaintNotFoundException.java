package com.ethiorental.backend.complaint.exception;

import java.util.UUID;

public class ComplaintNotFoundException extends RuntimeException {

    public ComplaintNotFoundException(UUID id) {
        super("Complaint not found: " + id);
    }
}
