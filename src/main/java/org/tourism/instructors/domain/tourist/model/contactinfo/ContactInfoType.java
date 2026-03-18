package org.tourism.instructors.domain.tourist.model.contactinfo;

import lombok.Getter;

@Getter
public enum ContactInfoType {
    PHONE_NUMBER(null),
    EMAIL(null),
    TELEGRAM(TelegramDetails.class);

    private final Class<? extends ContactInfoDetails> detailsClass;

    ContactInfoType(Class<? extends ContactInfoDetails> detailsClass) {
        this.detailsClass = detailsClass;
    }

}
