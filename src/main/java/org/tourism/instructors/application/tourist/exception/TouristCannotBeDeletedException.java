package org.tourism.instructors.application.tourist.exception;

import lombok.Getter;

@Getter
public class TouristCannotBeDeletedException extends RuntimeException {
    private final int touristId;

    public TouristCannotBeDeletedException (String message, int touristId) {
        super(message);
        this.touristId = touristId;
    }

}
