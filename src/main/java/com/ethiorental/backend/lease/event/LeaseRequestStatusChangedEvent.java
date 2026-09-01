package com.ethiorental.backend.lease.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

@Getter
public class LeaseRequestStatusChangedEvent extends ApplicationEvent {
    private final UUID leaseRequestId;
    private final String requestCode;
    private final String oldStatus;
    private final String newStatus;
    private final String tenantName;
    private final String tenantEmail;
    private final UUID propertyId;
    private final String propertyCode;
    private final String propertyTitle;
    private final String landlordName;
    private final String landlordEmail;

    public LeaseRequestStatusChangedEvent(Object source, UUID leaseRequestId, String requestCode,
                                          String oldStatus, String newStatus,
                                          String tenantName, String tenantEmail,
                                          UUID propertyId, String propertyCode, String propertyTitle,
                                          String landlordName, String landlordEmail) {
        super(source);
        this.leaseRequestId = leaseRequestId;
        this.requestCode = requestCode;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.tenantName = tenantName;
        this.tenantEmail = tenantEmail;
        this.propertyId = propertyId;
        this.propertyCode = propertyCode;
        this.propertyTitle = propertyTitle;
        this.landlordName = landlordName;
        this.landlordEmail = landlordEmail;
    }
}
