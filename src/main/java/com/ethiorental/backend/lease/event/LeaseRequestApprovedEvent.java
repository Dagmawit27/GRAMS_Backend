package com.ethiorental.backend.lease.event;

import org.springframework.context.ApplicationEvent;

import java.util.UUID;

public class LeaseRequestApprovedEvent extends ApplicationEvent {
    private final UUID leaseRequestId;
    private final String requestCode;
    private final String supervisorName;
    private final String supervisorEmail;
    private final String tenantName;
    private final String tenantEmail;
    private final String landlordName;
    private final String landlordEmail;
    private final String propertyCode;
    private final String propertyTitle;

    public LeaseRequestApprovedEvent(Object source, UUID leaseRequestId, String requestCode,
                                      String supervisorName, String supervisorEmail,
                                      String tenantName, String tenantEmail,
                                      String landlordName, String landlordEmail,
                                      String propertyCode, String propertyTitle) {
        super(source);
        this.leaseRequestId = leaseRequestId;
        this.requestCode = requestCode;
        this.supervisorName = supervisorName;
        this.supervisorEmail = supervisorEmail;
        this.tenantName = tenantName;
        this.tenantEmail = tenantEmail;
        this.landlordName = landlordName;
        this.landlordEmail = landlordEmail;
        this.propertyCode = propertyCode;
        this.propertyTitle = propertyTitle;
    }

    public UUID getLeaseRequestId() {
        return leaseRequestId;
    }

    public String getRequestCode() {
        return requestCode;
    }

    public String getSupervisorName() {
        return supervisorName;
    }

    public String getSupervisorEmail() {
        return supervisorEmail;
    }

    public String getTenantName() {
        return tenantName;
    }

    public String getTenantEmail() {
        return tenantEmail;
    }

    public String getLandlordName() {
        return landlordName;
    }

    public String getLandlordEmail() {
        return landlordEmail;
    }

    public String getPropertyCode() {
        return propertyCode;
    }

    public String getPropertyTitle() {
        return propertyTitle;
    }
}
