package org.tourism.instructors.api.tourist.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import org.tourism.instructors.domain.tourist.model.Gender;

import java.time.LocalDate;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class TouristDTO {
    private int id;
    private String certificationId;
    private String firstName;
    private String lastName;
    private String middleName;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateOfBirth;
    private Gender gender;
    private String phoneNumber;
    private String email;

    public String getFullName() {
        return lastName + " " + firstName + " " + middleName;
    }

    public boolean isNewItem() {
        return id == 0;
    }
}
