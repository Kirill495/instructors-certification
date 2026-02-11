package org.tourism.instructors.api.tourist.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class TouristLightDTO {
    private int id;
    private String firstName;
    private String lastName;
    private String middleName;
    private String certificationId;

    public String getView() {
        return lastName + " " + firstName + " " + middleName + " ("+certificationId+")";
    }

    public String getFullName() {
        return lastName + " " + firstName + " " + middleName;
    }
}
