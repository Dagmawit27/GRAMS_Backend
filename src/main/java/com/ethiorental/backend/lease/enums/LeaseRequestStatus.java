package com.ethiorental.backend.lease.enums;

public enum LeaseRequestStatus {
    PENDING,
    LANDLORD_APPROVED,
    UNDER_VERIFICATION,
    PENDING_SUPERVISOR_APPROVAL,
    SUPERVISOR_APPROVED,
    REJECTED,
    CANCELLED,
    EXPIRED,
    @Deprecated
    APPROVED;

    public static LeaseRequestStatus fromString(String value) {
        if (value == null) return null;
        String normalized = value.trim().toUpperCase();
        if ("APPROVED".equals(normalized)) {
            return LANDLORD_APPROVED;
        }
        return LeaseRequestStatus.valueOf(normalized);
    }
}
