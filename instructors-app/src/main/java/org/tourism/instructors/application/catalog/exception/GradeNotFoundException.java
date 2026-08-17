package org.tourism.instructors.application.catalog.exception;

public class GradeNotFoundException extends RuntimeException {
    public GradeNotFoundException(int id) {
        super("Звание с ID:" + id + " не найдено");
    }
}
