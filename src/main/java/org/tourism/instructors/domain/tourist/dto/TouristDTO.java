package org.tourism.instructors.domain.tourist.dto;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TouristDTO {
    private int id;
    private String fullName;

    public TouristDTO (int id, String fullName) {
        this.id = id;
        this.fullName = fullName;
    }
}
