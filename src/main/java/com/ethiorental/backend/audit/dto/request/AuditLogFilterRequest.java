package com.ethiorental.backend.audit.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogFilterRequest {
    private String module;
    private String actorId;
    private String action;
    private String outcome;
    private Instant startDate;
    private Instant endDate;
    @Builder.Default
    private int page = 0;
    @Builder.Default
    private int size = 20;
}
