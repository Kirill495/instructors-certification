package org.tourism.instructors.api.tourist.dto;


import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.tourism.instructors.api.catalog.dto.GradeDTO;
import org.tourism.instructors.api.catalog.dto.KindOfTourismDTO;
import org.tourism.instructors.domain.tourist.model.Gender;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class TouristDTO {
    private Integer id;
    private String certificationId;
    private String firstName;
    private String lastName;
    private String middleName;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateOfBirth;
    private Gender gender;
    private List<AssignmentDTO> assignments;
    private List<ContactInfoItemDTO> contactInfo;

    public String getFullName() {
        return Stream.of(lastName, firstName, middleName).filter(Objects::nonNull).collect(Collectors.joining(" "));
    }

    public boolean isNewItem() {
        return id == null;
    }

    public record AssignmentDTO(
            int protocolId,
            LocalDate date,
            LocalDate validThrough,
            GradeDTO grade,
            KindOfTourismDTO kindOfTourism,
            boolean valid) {
    }
}
