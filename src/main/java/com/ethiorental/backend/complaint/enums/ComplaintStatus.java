package com.ethiorental.backend.complaint.enums;

public enum ComplaintStatus {
    /** Submitted by citizen, not yet reviewed */
    SUBMITTED,
    /** Under review — an officer has been assigned */
    UNDER_INVESTIGATION,
    /** Resolved — resolution recorded */
    RESOLVED,
    /** Closed without resolution (e.g. duplicate, out-of-scope) */
    CLOSED,
    /** Withdrawn by the complainant */
    WITHDRAWN
}
