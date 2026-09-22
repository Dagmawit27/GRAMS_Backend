package com.ethiorental.backend.lease.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

@Getter
public class LeaseRequestSignedEvent extends ApplicationEvent {
    private final UUID leaseRequestId;
    private final String requestCode;
    private final String signerType; // "landlord", "tenant", "supervisor"
    private final String tenantName;
    private final String tenantEmail;
    private final String landlordName;
    private final String landlordEmail;
    private final String propertyCode;
    private final String propertyTitle;
    private final String leaseRequestStatus;
    private final String subCity;
    private final String woreda;
    private final boolean landlordSigned;
    private final boolean tenantSigned;
    private final boolean bothSigned;

    public LeaseRequestSignedEvent(Object source, UUID leaseRequestId, String requestCode,
                                   String signerType,
                                   String tenantName, String tenantEmail,
                                   String landlordName, String landlordEmail,
                                   String propertyCode, String propertyTitle,
                                   String leaseRequestStatus,
                                   String subCity, String woreda,
                                   boolean landlordSigned, boolean tenantSigned, boolean bothSigned) {
        super(source);
        this.leaseRequestId = leaseRequestId;
        this.requestCode = requestCode;
        this.signerType = signerType;
        this.tenantName = tenantName;
        this.tenantEmail = tenantEmail;
        this.landlordName = landlordName;
        this.landlordEmail = landlordEmail;
        this.propertyCode = propertyCode;
        this.propertyTitle = propertyTitle;
        this.leaseRequestStatus = leaseRequestStatus;
        this.subCity = subCity;
        this.woreda = woreda;
        this.landlordSigned = landlordSigned;
        this.tenantSigned = tenantSigned;
        this.bothSigned = bothSigned;
    }
}
