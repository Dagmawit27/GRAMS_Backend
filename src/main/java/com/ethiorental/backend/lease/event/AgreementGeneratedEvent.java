package com.ethiorental.backend.lease.event;

import com.ethiorental.backend.IAM.entity.Citizen;
import com.ethiorental.backend.property.entity.Property;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;

public class AgreementGeneratedEvent extends ApplicationEvent {
    private final String requestCode;
    private final Citizen landlord;
    private final Citizen tenant;
    private final Property property;
    private final LocalDateTime generatedAt;

    public AgreementGeneratedEvent(Object source, String requestCode, Citizen landlord, Citizen tenant, Property property) {
        super(source);
        this.requestCode = requestCode;
        this.landlord = landlord;
        this.tenant = tenant;
        this.property = property;
        this.generatedAt = LocalDateTime.now();
    }

    public String getRequestCode() {
        return requestCode;
    }

    public Citizen getLandlord() {
        return landlord;
    }

    public Citizen getTenant() {
        return tenant;
    }

    public Property getProperty() {
        return property;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }
}
