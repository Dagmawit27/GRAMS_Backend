package com.ethiorental.backend.property.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

@Getter
public class PropertyDeletedEvent extends ApplicationEvent {
    private final UUID propertyId;
    private final String propertyCode;
    private final String propertyTitle;
    private final String city;
    private final String subCity;
    private final String woreda;

    public PropertyDeletedEvent(Object source, UUID propertyId, String propertyCode, String propertyTitle,
                                 String city, String subCity, String woreda) {
        super(source);
        this.propertyId = propertyId;
        this.propertyCode = propertyCode;
        this.propertyTitle = propertyTitle;
        this.city = city;
        this.subCity = subCity;
        this.woreda = woreda;
    }
}
