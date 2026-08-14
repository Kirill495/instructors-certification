package org.tourism.instructors.api.tourist.dto;

import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record TouristSummaryDTO(
        int id, String firstName, String lastName, String middleName, String certificationId) {
    public String getView() {
        return getFullName() + " (" + certificationId + ")";
    }

    public String getFullName() {
        return Stream.of(lastName, firstName, middleName)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(" "));
    }
}
