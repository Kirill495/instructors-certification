package org.tourism.instructors.api.tourist.dto;

import org.tourism.instructors.domain.tourist.model.contactinfo.ContactInfoDetails;
import org.tourism.instructors.domain.tourist.model.contactinfo.ContactInfoType;

public record ContactInfoItemDTO(
        Integer id, Integer touristId, ContactInfoType type, String value, ContactInfoDetails details
) {
    public ContactInfoItemDTO(ContactInfoType type, String value) {
        this(null, null, type, value, null);
    }

    public ContactInfoItemDTO(ContactInfoType type, String value, ContactInfoDetails details) {
        this(null, null, type, value, details);
    }
}
