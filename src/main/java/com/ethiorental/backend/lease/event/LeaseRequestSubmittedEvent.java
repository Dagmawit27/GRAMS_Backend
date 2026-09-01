package com.ethiorental.backend.lease.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

@Getter
public class LeaseRequestSubmittedEvent extends ApplicationEvent {
    private final UUID leaseRequestId;
    private final String requestCode;
    private final String tenantName;
    private final String tenantEmail;
    private final String tenantPhone;
    private final UUID propertyId;
    private final String propertyCode;
    private final String propertyTitle;
    private final String landlordId;
    private final String landlordEmail;
    private final String monthlyRent;

    public LeaseRequestSubmittedEvent(Object source, UUID leaseRequestId, String requestCode,
                                       String tenantName, String tenantEmail, String tenantPhone,
                                       UUID propertyId, String propertyCode, String propertyTitle,
                                       String landlordId, String landlordEmail, String monthlyRent) {
        super(source);
        this.leaseRequestId = leaseRequestId;
        this.requestCode = requestCode;
        this.tenantName = tenantName;
        this.tenantEmail = tenantEmail;
        this.tenantPhone = tenantPhone;
        this.propertyId = propertyId;
        this.propertyCode = propertyCode;
        this.propertyTitle = propertyTitle;
        this.landlordId = landlordId;
        this.landlordEmail = landlordEmail;
        this.monthlyRent = monthlyRent;
    }
}
