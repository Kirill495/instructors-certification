package org.tourism.instructors.api.tourist.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import org.tourism.instructors.api.catalog.dto.GradeDTO;
import org.tourism.instructors.api.catalog.dto.KindOfTourismDTO;
import org.tourism.instructors.domain.tourist.model.Gender;

import java.time.LocalDate;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class TouristDTO {
    private Integer id;
    private String certificationId;
    private String firstName;
    private String lastName;
    private String middleName;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateOfBirth;
    private Gender gender;
    private String phoneNumber;
    private String email;
    private List<AssignmentDTO> assignments;
    public String getFullName() {
        return lastName + " " + firstName + " " + middleName;
    }

    public boolean isNewItem() {
        return id == 0;
    }

    @NoArgsConstructor
    @Getter
    @Setter
    public static class AssignmentDTO {
        private int protocolId;
        @DateTimeFormat(pattern = "yyyy-MM-dd")
        private LocalDate date;
        @DateTimeFormat(pattern = "yyyy-MM-dd")
        private LocalDate validThrough;
        private GradeDTO grade;
        private KindOfTourismDTO kindOfTourism;
        private boolean valid;
    }
}
