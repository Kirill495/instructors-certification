package org.tourism.instructors.api.tourist.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.tourism.instructors.domain.tourist.model.contactinfo.ContactInfoDetails;
import org.tourism.instructors.domain.tourist.model.contactinfo.ContactInfoType;

@Getter
@Setter
@NoArgsConstructor
public class ContactInfoItemDTO {
    private Integer touristId;
    private ContactInfoType type;
    private String value;
    private ContactInfoDetails details;
}
