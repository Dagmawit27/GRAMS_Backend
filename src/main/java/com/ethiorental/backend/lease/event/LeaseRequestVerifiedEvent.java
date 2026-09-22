package com.ethiorental.backend.lease.event;

import org.springframework.context.ApplicationEvent;

import java.util.UUID;

public class LeaseRequestVerifiedEvent extends ApplicationEvent {
    private final UUID leaseRequestId;
    private final String requestCode;
    private final String officerName;
    private final String officerEmail;
    private final String tenantName;
    private final String tenantEmail;
    private final String landlordName;
    private final String landlordEmail;
    private final String propertyCode;
    private final String propertyTitle;
    private final String subCity;
    private final String woreda;

    public LeaseRequestVerifiedEvent(Object source, UUID leaseRequestId, String requestCode, 
                                     String officerName, String officerEmail,
                                     String tenantName, String tenantEmail,
                                     String landlordName, String landlordEmail,
                                     String propertyCode, String propertyTitle,
                                     String subCity, String woreda) {
        super(source);
        this.leaseRequestId = leaseRequestId;
        this.requestCode = requestCode;
        this.officerName = officerName;
        this.officerEmail = officerEmail;
        this.tenantName = tenantName;
        this.tenantEmail = tenantEmail;
        this.landlordName = landlordName;
        this.landlordEmail = landlordEmail;
        this.propertyCode = propertyCode;
        this.propertyTitle = propertyTitle;
        this.subCity = subCity;
        this.woreda = woreda;
    }

    public UUID getLeaseRequestId() {
        return leaseRequestId;
    }

    public String getRequestCode() {
        return requestCode;
    }

    public String getOfficerName() {
        return officerName;
    }

    public String getOfficerEmail() {
        return officerEmail;
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

    public String getSubCity() {
        return subCity;
    }

    public String getWoreda() {
        return woreda;
    }
}
