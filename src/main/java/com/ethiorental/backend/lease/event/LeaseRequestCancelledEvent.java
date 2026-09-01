package com.ethiorental.backend.lease.event;

import org.springframework.context.ApplicationEvent;

import java.util.UUID;

public class LeaseRequestCancelledEvent extends ApplicationEvent {

    private final UUID leaseRequestId;
    private final String requestCode;
    private final String tenantName;
    private final String tenantEmail;
    private final UUID propertyId;
    private final String propertyCode;
    private final String propertyTitle;
    private final String landlordId;
    private final String landlordEmail;

    public LeaseRequestCancelledEvent(
            Object source,
            UUID leaseRequestId,
            String requestCode,
            String tenantName,
            String tenantEmail,
            UUID propertyId,
            String propertyCode,
            String propertyTitle,
            String landlordId,
            String landlordEmail) {
        super(source);
        this.leaseRequestId = leaseRequestId;
        this.requestCode = requestCode;
        this.tenantName = tenantName;
        this.tenantEmail = tenantEmail;
        this.propertyId = propertyId;
        this.propertyCode = propertyCode;
        this.propertyTitle = propertyTitle;
        this.landlordId = landlordId;
        this.landlordEmail = landlordEmail;
    }

    public UUID getLeaseRequestId() {
        return leaseRequestId;
    }

    public String getRequestCode() {
        return requestCode;
    }

    public String getTenantName() {
        return tenantName;
    }

    public String getTenantEmail() {
        return tenantEmail;
    }

    public UUID getPropertyId() {
        return propertyId;
    }

    public String getPropertyCode() {
        return propertyCode;
    }

    public String getPropertyTitle() {
        return propertyTitle;
    }

    public String getLandlordId() {
        return landlordId;
    }

    public String getLandlordEmail() {
        return landlordEmail;
    }
}
