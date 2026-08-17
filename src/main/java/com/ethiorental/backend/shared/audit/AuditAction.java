package com.ethiorental.backend.shared.audit;

public enum AuditAction {
    REGISTER,
    LOGIN,
    LOGOUT,
    LOGIN_FAILED,
    LOCKOUT,
    CREATE,
    UPDATE,
    DELETE,
    APPROVE,
    REJECT,
    VERIFY,
    ROLE_CHANGE,
    CONFIG_CHANGE,
    PAYMENT_RECORDED,
    DOCUMENT_ACCESS,
    DATA_EXPORT
}
