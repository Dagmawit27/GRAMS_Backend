package com.ethiorental.backend.lease.dto;

import com.ethiorental.backend.lease.enums.LeaseRequestStatus;
import jakarta.validation.constraints.NotNull;

public record LeaseStatusUpdateRequest(
        @NotNull LeaseRequestStatus newStatus,
        String remarks
) {}
