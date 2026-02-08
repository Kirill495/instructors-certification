package org.tourism.instructors.api.catalog.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class GradeDTO {
    private int id;
    private String title;
    private boolean inactive;
    private int expiresInYears;
}
