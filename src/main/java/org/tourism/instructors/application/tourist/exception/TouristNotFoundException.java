package org.tourism.instructors.application.tourist.exception;

public class TouristNotFoundException extends RuntimeException {
    int id;
    public TouristNotFoundException (int touristId) {
        super("Турист с id: " + touristId + " не найден");
        this.id = id;
    }
}
