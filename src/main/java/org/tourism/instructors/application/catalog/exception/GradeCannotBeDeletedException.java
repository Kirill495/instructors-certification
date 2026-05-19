package org.tourism.instructors.application.catalog.exception;

import lombok.Getter;

@Getter
public abstract class GradeCannotBeDeletedException extends RuntimeException {
    private final int gradeId;

    public GradeCannotBeDeletedException(String message, int gradeId) {
        super(message);
        this.gradeId = gradeId;
    }

}
