package com.ethiorental.backend.audit.exception;

public class AuditWriteException extends RuntimeException {
    public AuditWriteException(String message, Throwable cause) {
        super(message, cause);
    }
}
