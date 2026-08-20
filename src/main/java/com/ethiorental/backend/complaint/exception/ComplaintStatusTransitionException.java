package com.ethiorental.backend.complaint.exception;

public class ComplaintStatusTransitionException extends RuntimeException {

    public ComplaintStatusTransitionException(String message) {
        super(message);
    }
}
