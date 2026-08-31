package com.ethiorental.backend.property.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

@Getter
public class PropertyRegisteredEvent extends ApplicationEvent {
    private final UUID propertyId;
    private final String propertyCode;
    private final String propertyTitle;
    private final String propertyType;
    private final String city;
    private final String subCity;
    private final String woreda;
    private final String landlordId;
    private final String landlordName;

    public PropertyRegisteredEvent(Object source, UUID propertyId, String propertyCode, String propertyTitle, 
                                   String propertyType, String city, String subCity, String woreda, 
                                   String landlordId, String landlordName) {
        super(source);
        this.propertyId = propertyId;
        this.propertyCode = propertyCode;
        this.propertyTitle = propertyTitle;
        this.propertyType = propertyType;
        this.city = city;
        this.subCity = subCity;
        this.woreda = woreda;
        this.landlordId = landlordId;
        this.landlordName = landlordName;
    }
}
