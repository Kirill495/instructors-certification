package org.tourism.instructors.api.catalog.dto;

import java.util.Objects;

public record GradeDTO(Integer id, String title, boolean inactive, int expiresInYears) {
    public boolean isNewItem() {
        return Objects.isNull(id);
    }
}
