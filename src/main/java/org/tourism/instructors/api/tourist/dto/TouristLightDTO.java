package org.tourism.instructors.api.tourist.dto;

public record TouristLightDTO(
        int id,
        String firstName,
        String lastName,
        String middleName,
        String certificationId
) {
    public String getView() {
        return lastName + " " + firstName + " " + middleName + " (" + certificationId + ")";
    }

    public String getFullName() {
        return lastName + " " + firstName + " " + middleName;
    }
}
