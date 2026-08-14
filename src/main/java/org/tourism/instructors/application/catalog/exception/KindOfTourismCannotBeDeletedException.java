package org.tourism.instructors.application.catalog.exception;

import lombok.Getter;

@Getter
public abstract class KindOfTourismCannotBeDeletedException extends RuntimeException {
    private final int kindOfTourismId;

    public KindOfTourismCannotBeDeletedException(String message, int kindOfTourismId) {
        super(message);
        this.kindOfTourismId = kindOfTourismId;
    }
}
